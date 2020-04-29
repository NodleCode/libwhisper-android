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

package world.coalition.whisper

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.id.SecureTid
import world.coalition.whisper.id.SessionKeyParam
import world.coalition.whisper.id.TidGenerator
import world.coalition.whisper.agathe.BleScanner
import world.coalition.whisper.database.BoundingBox
import world.coalition.whisper.database.PeerContactEvent
import world.coalition.whisper.database.WhisperDatabase
import world.coalition.whisper.exceptions.WhisperAlreadyStartedException
import world.coalition.whisper.exceptions.WhisperNotStartedException
import world.coalition.whisper.geo.GeoUtil
import world.coalition.whisper.geo.GpsLogger


/**
 * @author Lucien Loiseau on 03/04/20.
 */
class WhisperCore(val context: Context) : Whisper {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)

    var whisperConfig: WhisperConfig = WhisperConfig()

    var db: WhisperDatabase = WhisperDatabase.persistent(context)

    private var gpsLogger: GpsLogger? = null

    private var coreJob: Job? = null

    private var bleScanner: BleScanner? = null

    lateinit var channel: Channel<PeerContactEvent>
        private set

    /** Public Methods - interface implementation **/

    @Throws(WhisperAlreadyStartedException::class)
    override fun start() = start(WhisperConfig())

    @Throws(WhisperAlreadyStartedException::class)
    override fun start(config: WhisperConfig): Whisper {
        if (coreJob != null) throw WhisperAlreadyStartedException()
        log.debug("[+] starting lib whisper..")

        coreJob = CoroutineScope(Dispatchers.IO).launch {
            gpsLogger = GpsLogger(this@WhisperCore)

            bleScanner = BleScanner(this@WhisperCore)

            channel = Channel(capacity = Channel.UNLIMITED)

            gpsLogger?.start()

            bleScanner?.start(channel)

            for (contact in channel) db.addContact(contact)
        }
        return this
    }

    @Throws(WhisperNotStartedException::class)
    override suspend fun stop() {
        if (coreJob == null) throw WhisperNotStartedException()
        log.debug("[+] stopping lib whisper..")
        gpsLogger?.stop()
        bleScanner?.stop()
        channel.close()
        coreJob!!.join()
        db.close()
    }

    override fun isStarted(): Boolean {
        return coreJob != null
    }

    fun getGenerator(): TidGenerator {
        return db
            .getCurrentGenerator(System.currentTimeMillis() / 1000, whisperConfig.sessionKeyValiditySec, whisperConfig.temporaryIdValiditySec)
    }

    fun getSecureId(challenge: ByteArray): SecureTid {
        return db
            .getCurrentGenerator(System.currentTimeMillis() / 1000, whisperConfig.sessionKeyValiditySec, whisperConfig.temporaryIdValiditySec)
            .generateTidWithChallenge(challenge)
    }

    override suspend fun extractLastPeriodSessionKeys(periodSec: Long): List<SessionKeyParam> {
        val localSk = db.roomDb.sessionKeyDao().getAllLocal()
        val from = (System.currentTimeMillis()/1000) - periodSec
        return localSk.filter {
            (it.tr > from)
        }.map {
            it.toSecretParam()
        }
    }

    override suspend fun evictLocalKey(evicted: SessionKeyParam) =
        db.evictLocalKeys(listOf(evicted))

    override suspend fun getPrivacyBox(periodSec: Long): BoundingBox? {
        val perfectFit = db.roomDb.locationUpdateDao().boundingBox(periodSec*1000)
        perfectFit ?:return null
        if(perfectFit.minLatitude == 0.0 && perfectFit.minLongitude == 0.0) return null
        return GeoUtil.fuzzyBoundingBox(perfectFit, whisperConfig.minimumBoundingBoxSize.coerceAtLeast(100000))
    }

    // may be cpu intensive
    // TODO find ways to optimize this computation
    override suspend fun processTaintedKeys(keys: List<SessionKeyParam>, risk: String) =
        db.processInfectedKeys(keys, risk, whisperConfig.sessionKeyValiditySec)

    override suspend fun getRiskExposure(tag: String): Int =
        db.getInfectionExposure(tag)
}