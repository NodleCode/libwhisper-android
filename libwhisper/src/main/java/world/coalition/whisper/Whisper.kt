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
import world.coalition.whisper.exceptions.WhisperAlreadyStartedException
import world.coalition.whisper.exceptions.WhisperNotStartedException

/**
 * @author Lucien Loiseau on 03/04/20.
 */
interface Whisper {

    companion object {
        var core: WhisperCore? = null

        fun instance(): Whisper {
            return core?:WhisperCore()
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
    fun start(context: Context): Whisper

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
    fun start(context: Context, config: WhisperConfig): Whisper

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
     * Extract the last tell tokens that hasn't been shared yet.
     * This is the list of tokens you submit if user has tested
     * positive to a risk.
     *
     * @param period in seconds during which session keys were used
     * @return the tell-token set
     */
    suspend fun getLastTellTokens(context: Context, periodSec: Long): List<GeoToken>

    /**
     * Mark those tokens as shared.
     *
     * @param tokens the tell-token set
     */
    suspend fun tellTokensShared(context: Context, tokens: List<String>)

    /**
     * Extract the last hear tokens. This is the list of tokens you submit to the match-maker
     * to query about current user risk status.
     *
     * @param period in seconds during which session keys were used
     * @return the hear-token set
     */
    suspend fun getLastHearTokens(context: Context, periodSec: Long): List<GeoToken>

    /**
     * process a hear-token set (peer's tell token) to perform match-making locally.
     *
     * @param infectedSet the list of tell tokens
     * @param tag of the risk, for instance "covid-19"
     */
    suspend fun processHearTokens(context: Context, infectedSet: List<String>, tag: String): Int

    /**
     * returns the total number of interaction since a specific timestamp in msec
     *
     * @param sinceTimestampMs: The date from which to count interactions
     */
    suspend fun getNumberOfInteractions(context: Context, sinceTimestampMs: Long): Long

    /**
     * returns the total number of interaction whose got labelled with a risk
     * since a specific timestamp in msec
     *
     * @param tag of the risk, for instance "covid-19"
     * @param sinceTimestampMs: The date from which to count interactions
     */
    suspend fun getNumberOfRiskInteractions(context: Context, tag: String, sinceTimestampMs: Long): Long

    /**
     * return an estimate of the exposure against a given risk.
     * This method only works in a decentralized scheme if current node where fed tokens.
     *
     * @param tag of the risk, for instance "covid-19"
     * @param sinceTimestampMs: The date from which to count interactions
     */
    suspend fun getRiskExposure(context: Context, tag: String, sinceTimestampMs: Long): Int

}