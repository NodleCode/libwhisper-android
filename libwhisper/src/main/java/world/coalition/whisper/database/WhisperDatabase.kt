/*
 * This file is part of the Whisper Protocol distributed at https://github.com/NodleCode/whisper-tracing-android
 * Copyright (C) 2020  Coalition Network
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package world.coalition.whisper.database

import android.content.Context
import android.util.Base64
import androidx.room.Room
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.Whisper
import world.coalition.whisper.id.KdfAlgorithm
import world.coalition.whisper.id.SessionKeyParam
import world.coalition.whisper.id.TidGenerator
import world.coalition.whisper.id.TidGeneratorBlake2B

/**
 * @author Lucien Loiseau on 04/04/20.
 */
class WhisperDatabase private constructor(val context: Context) {

    companion object {
        val localTag: String = "user-sk"

        val unknownSessionKey: SessionKey = SessionKey(
            "unknown",
            1,
            0,
            0,
            0,
            "unknown",
            true,
            "DEFAULT_KEY_FOR_PEER_TABLE_FOREIGN_KEY"
        )

        // for production purpose
        fun persistent(context: Context): WhisperDatabase {
            val ret = WhisperDatabase(context)
            ret.roomDb = Room.databaseBuilder(
                context,
                RoomDatabase::class.java,
                "whisper"
            ).build()
            ret.retrieveLastGenerator()
            return ret
        }

        // for test instrumentation
        fun memory(context: Context): WhisperDatabase {
            val ret = WhisperDatabase(context)
            ret.roomDb = Room.inMemoryDatabaseBuilder(
                context,
                RoomDatabase::class.java
            ).allowMainThreadQueries().build()
            ret.retrieveLastGenerator()
            return ret
        }
    }

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)
    lateinit var roomDb: RoomDatabase
        private set

    val unknownSessionKeyRowId: Long by lazy {
        roomDb.sessionKeyDao()
            .getLast(1, unknownSessionKey.tag)
            ?.let {
                it.row_id
            }
            ?: let {
                roomDb.sessionKeyDao().insert(unknownSessionKey)
            }
    }

    private var idGen: TidGenerator? = null

    fun close() {
        roomDb.close()
    }

    private fun retrieveLastGenerator() {
        idGen = roomDb.sessionKeyDao().getLast(1, localTag)?.let {
            TidGeneratorBlake2B(it.toSecretParam())
        } // may still be null if no key in the db
    }

    private fun createNewGenerator(now: Long, expireAfterSec: Int, timeStepSec: Int) {
        idGen = TidGeneratorBlake2B.New(now, expireAfterSec, timeStepSec)
        log.info(">>> generating a new one: ${Base64.encodeToString(idGen!!.getSecretParam().SecretKey, Base64.NO_WRAP)}")
        roomDb.sessionKeyDao().insert(SessionKey(idGen!!.getSecretParam(), true, localTag))
    }

    fun getCurrentGenerator(now: Long, expireAfterSec: Int, timeStepSec: Int): TidGenerator {
        synchronized(this) {
            idGen = idGen ?: roomDb.sessionKeyDao().getLast(1, localTag)?.let {
                TidGeneratorBlake2B(it.toSecretParam())
            } // may still be null if no key in the db

            if (idGen == null || (idGen?.isExpire(now) != false)) {
                createNewGenerator(now, expireAfterSec, timeStepSec)
            }
        }
        return idGen!!
    }

    suspend fun extractLastSessionKeys(n: Int): List<SessionKeyParam> {
        return roomDb.sessionKeyDao()
            .getLast(1, localTag, n)
            .map {
                it.toSecretParam()
            }
    }

    fun evictLocalKeys(evicted: List<SessionKeyParam>) {
        synchronized(this) {
            val currentGen = idGen
            for (key in evicted) {
                log.info(
                    ">>> evicting key: ${Base64.encodeToString(
                        key.SecretKey,
                        Base64.NO_WRAP
                    )}"
                )
                if (currentGen != null && currentGen.getSecretParam().SecretKey.contentEquals(key.SecretKey)) {
                    createNewGenerator(
                        System.currentTimeMillis(),
                        currentGen.getSecretParam().ExpireAfterSec,
                        currentGen.getSecretParam().TimeStepSec
                    )
                }
                roomDb.sessionKeyDao()
                    .delete(Base64.encodeToString(key.SecretKey, Base64.NO_WRAP))
            }
        }
    }

    /* contact processor */
    fun addContact(peerContactEvent: PeerContactEvent): Long {
        // grab the rowid for this peer
        val peerTidRowId =
            updateOrInsertPeerId(peerContactEvent.peerTid, peerContactEvent.connectTimeMillis)

        // add a ping entry
        // a ping entry from a contact event is always inserted with an elapsed time of 0
        roomDb.peerPingEventDao()
            .insert(PeerPingEvent(peerTidRowId, peerContactEvent.connectTimeMillis, 0))

        // record contact entry
        peerContactEvent.advPeerTidRowId = peerTidRowId
        return roomDb.peerContactEventDao().insert(peerContactEvent)
    }

    fun addPing(peripheralId: String, timeMs: Long, pingMaxElapsed: Long): Long? {
        val lastConnect = roomDb.peerContactEventDao()
            .getLastConnect(PeerContactEvent.Base64SHA256(peripheralId))
        val peerTidRowId = lastConnect?.advPeerTidRowId ?: return null
        return addPing(peerTidRowId, timeMs, pingMaxElapsed)
    }

    fun addPing(peerTidRowId: Long, timeMs: Long, pingMaxElapsed: Long): Long? {
        val lastPing = roomDb.peerPingEventDao().getLast(peerTidRowId)
        val elapsed = lastPing?.pingTimestampMs?.minus(timeMs)?.times(-1) ?: -1
        if (elapsed < pingMaxElapsed) {
            return roomDb.peerPingEventDao().insert(PeerPingEvent(peerTidRowId, timeMs, elapsed))
        }
        return null
    }


    private fun updateOrInsertPeerId(peerId: String, seen: Long): Long {
        return roomDb.peerTidDao()
            .getRowId(peerId)
            ?.let {
                roomDb.peerTidDao().updateLastSeen(peerId, seen)
                it
            }
            ?: let {
                roomDb.peerTidDao().insert(PeerTid(peerId, seen, unknownSessionKeyRowId))
            }
    }

    fun processInfectedKeys(
        infected: List<SessionKeyParam>,
        tag: String,
        forceKeyExpirySec: Int
    ): Int {
        val start = System.currentTimeMillis()
        roomDb.whisperEventDao().insert(
            WhisperEvent(
                start,
                EventCode.PROCESS_KEYS_START.code, infected.size, 0, tag
            )
        )
        var nbOfMatches = 0
        for (keyParam in infected.filter { it.KdfId == KdfAlgorithm.BLAKE2B160.code }) {
            val gen = TidGeneratorBlake2B(keyParam)
            val nbValidKeys =
                (keyParam.ExpireAfterSec.coerceAtMost(forceKeyExpirySec) / keyParam.TimeStepSec)
            var skRowId: Long? = null

            for (keyId in 0..nbValidKeys) {
                val tid = Base64.encodeToString(gen.generateNthTid(keyId.toLong()), Base64.NO_WRAP)
                roomDb.peerTidDao().getRowId(tid)?.let {
                    //  ( ✜︵✜)  !!  it's a match! we have met an infected individual

                    // we add the session key if it wasn't inserted already
                    skRowId = skRowId
                        ?: roomDb.sessionKeyDao().insert(SessionKey(keyParam, false, tag))

                    // we update the relationship between the sessionkey and the tid
                    // TODO test wether inserting using rowid performs better than using tid
                    roomDb.peerTidDao().updateSessionKey(tid, skRowId!!)
                    nbOfMatches++
                }
            }
        }
        val stop = System.currentTimeMillis()
        roomDb.whisperEventDao().insert(
            WhisperEvent(
                stop,
                EventCode.PROCESS_KEYS_STOP.code, (stop - start).toInt(), nbOfMatches, tag
            )
        )
        return nbOfMatches
    }

    suspend fun getInfectionExposure(tag: String): Int {
        return roomDb.sessionKeyDao().estimateExposure(tag)
    }
}