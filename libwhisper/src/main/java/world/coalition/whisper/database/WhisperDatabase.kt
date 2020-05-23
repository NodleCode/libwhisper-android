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
import world.coalition.whisper.id.ECUtil
import world.coalition.whisper.id.KeyPairParam
import world.coalition.whisper.id.ProofOfInteraction
import java.security.KeyPair
import java.security.PublicKey

/**
 * @author Lucien Loiseau on 04/04/20.
 */
class WhisperDatabase private constructor() {

    companion object {
        // for production purpose
        fun persistent(context: Context): WhisperDatabase {
            val ret = WhisperDatabase()
            ret.roomDb = Room.databaseBuilder(
                context,
                RoomDatabase::class.java,
                "whisper"
            ).fallbackToDestructiveMigration().build()
            return ret
        }

        // for test instrumentation
        fun memory(context: Context): WhisperDatabase {
            val ret = WhisperDatabase()
            ret.roomDb = Room.inMemoryDatabaseBuilder(
                context,
                RoomDatabase::class.java
            ).fallbackToDestructiveMigration().allowMainThreadQueries().build()
            return ret
        }
    }

    lateinit var roomDb: RoomDatabase
        private set

    private var keyPairParam: KeyPairParam? = null

    fun close() {
        roomDb.close()
    }

    fun getCurrentKeyPair(time: Long, expireAfterSec: Int): KeyPair {
        keyPairParam = keyPairParam ?: roomDb.userKeyPairDao().getLast()?.toKeyPairParam()
        // may still be null if no key in the db

        if (keyPairParam == null || (keyPairParam?.isExpire(time) != false)) {
            keyPairParam = KeyPairParam(ECUtil.generateKeyPair(), time, expireAfterSec)
            roomDb.userKeyPairDao().insert(UserKeyPair(keyPairParam!!))
        }
        return keyPairParam!!.keyPair
    }

    fun getCurrentPublicKey(time: Long, expireAfterSec: Int): PublicKey {
        return getCurrentKeyPair(time, expireAfterSec).public
    }

    /* contact processor */
    fun addInteraction(
        interactionEvent: BleConnectEvent,
        proof: ProofOfInteraction,
        geohash: String?): Long {

        // grab the rowid for this pubkey
        val petRowId =
            updateOrInsertPet(proof, geohash?:"", interactionEvent.connectTimeMillis)

        // add a ping entry
        // a ping entry from a contact event is always inserted with an elapsed time of 0
        roomDb.blePingEventDao()
            .insert(BlePingEvent(petRowId, interactionEvent.connectTimeMillis, interactionEvent.rssi, 0))

        // record contact entry
        interactionEvent.petRowId = petRowId
        return roomDb.bleConnectEventDao().insert(interactionEvent)
    }

    fun addPing(peripheralId: String, rssi: Int, timeMs: Long, pingMaxElapsed: Long): Long? {
        val lastConnect = roomDb.bleConnectEventDao()
            .getLastConnect(BleConnectEvent.Base64SHA256(peripheralId))
        val petRowId = lastConnect?.petRowId ?: return null
        return addPing(petRowId, rssi, timeMs, pingMaxElapsed)
    }

    fun addPing(petRowId: Long, rssi: Int, timeMs: Long, pingMaxElapsed: Long): Long? {
        val lastPing = roomDb.blePingEventDao().getLast(petRowId)
        val elapsed = lastPing?.pingTimestampMs?.minus(timeMs)?.times(-1) ?: -1
        if (elapsed < pingMaxElapsed) { // TODO && check RSSI threshold
            return roomDb.blePingEventDao().insert(BlePingEvent(petRowId, timeMs, rssi, elapsed))
        }
        return null
    }

    private fun updateOrInsertPet(proof: ProofOfInteraction, geohash: String, seen: Long): Long {
        val tellToken = Base64.encodeToString(proof.tellToken, Base64.NO_WRAP)
        val hearToken = Base64.encodeToString(proof.hearToken, Base64.NO_WRAP)

        return roomDb.privateEncounterTokenDao()
            .getRowIdForHearToken(hearToken)
            ?.let {
                roomDb.privateEncounterTokenDao().updateLastSeen(it, seen)
                it
            }
            ?: let {
                roomDb.privateEncounterTokenDao().insert(
                    PrivateEncounterToken(tellToken, hearToken, geohash, seen, false, ""))
            }
    }

    fun tellTokensShared(shared: List<String>) {
        synchronized(this) {
            for (tellToken in shared) {
                roomDb.privateEncounterTokenDao().updateSharedStatus(tellToken, true)
            }
        }
    }

    fun processTellTokens(
        infected: List<String>,
        tag: String,
        since: Long
    ): Int {
        val start = System.currentTimeMillis()
        roomDb.whisperEventDao().insert(WhisperEvent(start, EventCode.PROCESS_KEYS_START.code, infected.size, 0, tag))
        var nbOfMatches = 0

        val tokens = HashMap<String, Long>()
        roomDb.privateEncounterTokenDao().getAllSince(since).map { tokens.set(it.hearToken, it.id) }
        for (peerTellToken in infected) {
            if (tokens.containsKey(peerTellToken)) {
                roomDb.privateEncounterTokenDao().updateTag(peerTellToken, tag)
                nbOfMatches++
            }
        }

        val stop = System.currentTimeMillis()
        roomDb.whisperEventDao().insert(WhisperEvent(stop, EventCode.PROCESS_KEYS_STOP.code, (stop - start).toInt(), nbOfMatches, tag))
        return nbOfMatches
    }

    suspend fun getRiskExposure(tag: String): Int {
        return roomDb.privateEncounterTokenDao().estimateRiskExposure(tag)
    }
}