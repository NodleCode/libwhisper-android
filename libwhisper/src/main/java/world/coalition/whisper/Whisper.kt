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
import world.coalition.whisper.id.SessionKeyParam
import world.coalition.whisper.database.BoundingBox
import world.coalition.whisper.exceptions.WhisperAlreadyStartedException
import world.coalition.whisper.exceptions.WhisperNotStartedException

/**
 * @author Lucien Loiseau on 03/04/20.
 */
interface Whisper {

    companion object {
        var core: WhisperCore? = null

        fun with(context: Context): Whisper {
            return core?:WhisperCore(context)
        }
    }

    /**
     * immediately start the whisper library:
     * - ble advertising
     * - ble periodic scanning
     * - ble connect and ID exchange
     *
     * @throws WhisperAlreadyStartedException if the library was already started
     * @param context Android Context
     */
    @Throws(WhisperAlreadyStartedException::class)
    fun start(): Whisper

    /**
     * immediately start the whisper library:
     * - ble advertising
     * - ble periodic scanning
     * - ble connect and ID exchange
     *
     * @throws WhisperAlreadyStartedException if the library was already started
     * @param context Android Context
     * @param config Whisper configuration
     */
    @Throws(WhisperAlreadyStartedException::class)
    fun start(config: WhisperConfig): Whisper

    /**
     * stops the whisper library. This method suspend the execution and only returns
     * once all the sub-component has shut down.
     *
     * @throws WhisperAlreadyStartedException if the library was not started
     */
    @Throws(WhisperNotStartedException::class)
    suspend fun stop()

    /**
     * check the status of the whisper library
     *
     * @return true if the library was started, false otherwise
     */
    fun isStarted(): Boolean

    /**
     * Extract the last session keys for this users.
     *
     * @param period in seconds during which session keys were used
     * @return the last n session keys or the maximum number of keys available
     */
    suspend fun extractLastPeriodSessionKeys(periodSec: Long): List<SessionKeyParam>

    /**
     * Extract the bounding box overlapping at least all of the user's past location for a given
     * time period. The bounding box is not a perfect fit and is at least 100km randomly
     * padded on each side.
     *
     * @param period in seconds describing the time slice to consider
     * @return a bounding box or null if no location data is available
     */
    suspend fun getPrivacyBox(periodSec: Long): BoundingBox?

    /**
     * Process keys that has been tagged as tainted with regards to a given risk. This method
     * will iterate over all the key generators supplied as parameters and checks against the
     * local db if there is any hits.
     *
     * @param keys the list of infected key generator
     * @param risk of the underlying risk associated to the keys
     * @return the number of "hits" with the local db.
     */
    suspend fun processTaintedKeys(keys: List<SessionKeyParam>, risk: String): Int

    /**
     * Evict local keys that has been exposed. An evicted key will no longer be returned
     * when calling extractLastPeriodSessionKeys. If the current key is part of the evicted
     * it will be mark as evicted and a new key will be generated
     *
     * @param evicted the key to evict
     */
    suspend fun evictLocalKey(evicted: SessionKeyParam)

    /**
     * return an estimate of the exposure against a given risk.
     *
     * @param tag of the risk, for instance "covid-19"
     */
    suspend fun getRiskExposure(tag: String): Int
}