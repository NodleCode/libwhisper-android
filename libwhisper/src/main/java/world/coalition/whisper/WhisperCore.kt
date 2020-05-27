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
import android.util.Base64
import ch.hsr.geohash.GeoHash
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.agathe.BleScanner
import world.coalition.whisper.database.BleConnectEvent
import world.coalition.whisper.database.WhisperDatabase
import world.coalition.whisper.exceptions.WhisperAlreadyStartedException
import world.coalition.whisper.exceptions.WhisperNotStartedException
import world.coalition.whisper.geo.GpsListener
import world.coalition.whisper.id.ECUtil
import java.security.KeyPair
import java.security.PublicKey


/**
 * @author Lucien Loiseau on 03/04/20.
 */
class WhisperCore : Whisper {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)

    private var db: WhisperDatabase? = null

    private var gpsListener: GpsListener? = null

    private var coreJob: Job? = null

    private var bleScanner: BleScanner? = null

    var channel: Channel<BleConnectEvent>? = null
        private set

    var whisperConfig: WhisperConfig = WhisperConfig()
        private set

    fun getDb(context: Context): WhisperDatabase {
        db = db ?: WhisperDatabase.persistent(context)
        return db!!
    }

    /** Public Methods - interface implementation **/
    @Throws(WhisperAlreadyStartedException::class)
    override fun start(context: Context) = start(context, WhisperConfig())

    @Throws(WhisperAlreadyStartedException::class)
    override fun start(context: Context, config: WhisperConfig): Whisper {
        if (coreJob != null) throw WhisperAlreadyStartedException()
        log.debug("[+] starting lib whisper..")

        coreJob = CoroutineScope(Dispatchers.IO).launch {
            var lastLocation: String? = null

            gpsListener = GpsListener(this@WhisperCore)
            bleScanner = BleScanner(this@WhisperCore)
            channel = Channel(capacity = Channel.UNLIMITED)
            gpsListener?.start(context) {
                lastLocation = GeoHash.withCharacterPrecision(it.latitude, it.longitude, 4).toBase32()
            }
            bleScanner?.start(context, channel!!)

            for (interaction in channel!!) {
                val proof = ECUtil.getInteraction(
                    getKeyPair(context),
                    Base64.decode(interaction.advPeerPubKey, Base64.NO_WRAP)
                )
                getDb(context).addInteraction(interaction, proof, lastLocation)
            }
        }
        return this
    }

    @Throws(WhisperNotStartedException::class)
    override suspend fun stop() {
        if (coreJob == null) throw WhisperNotStartedException()
        log.debug("[+] stopping lib whisper..")
        gpsListener?.stop()
        bleScanner?.stop()
        channel?.close()
        coreJob!!.join()
        db?.close()
    }

    override fun isStarted(): Boolean {
        return coreJob != null
    }

    fun getKeyPair(context: Context): AsymmetricCipherKeyPair {
        return getDb(context).getCurrentKeyPair(
            System.currentTimeMillis() / 1000,
            whisperConfig.pubkeyValidityPeriodSec)
    }

    fun getPublicKey(context: Context): X25519PublicKeyParameters {
        return getDb(context).getCurrentPublicKey(
            System.currentTimeMillis() / 1000,
            whisperConfig.pubkeyValidityPeriodSec)
    }

    override suspend fun getLastTellTokens(context: Context, periodSec: Long): List<GeoToken> {
        val sinceMsec = System.currentTimeMillis() - periodSec*1000
        return getDb(context).roomDb.privateEncounterTokenDao().getAllRemainingTellTokenSince(sinceMsec)
    }

    override suspend fun tellTokensShared(context: Context, tokens: List<String>) {
        getDb(context).tellTokensShared(tokens)
    }

    override suspend fun getLastHearTokens(context: Context, periodSec: Long): List<GeoToken> {
        val sinceMsec = System.currentTimeMillis() - periodSec*1000
        return getDb(context).roomDb.privateEncounterTokenDao().getAllHearTokenSince(sinceMsec)
    }

    override suspend fun processHearTokens(context: Context, infectedSet: List<String>, tag: String): Int {
        val sinceMsec = System.currentTimeMillis() - whisperConfig.incubationPeriod*1000
        return getDb(context).processTellTokens(infectedSet, tag,  sinceMsec)
    }

    override suspend fun getNumberOfInteractions(context: Context, sinceTimestampMs: Long): Long {
        return getDb(context).getNumberOfInteractions(sinceTimestampMs)
    }

    override suspend fun getNumberOfRiskInteractions(context: Context, tag: String, sinceTimestampMs: Long): Long {
        return getDb(context).getNumberOfRiskInteractions(tag, sinceTimestampMs)
    }

    override suspend fun getRiskExposure(context: Context, tag: String, sinceTimestampMs: Long): Int =
        getDb(context).getRiskExposure(tag, sinceTimestampMs)

}