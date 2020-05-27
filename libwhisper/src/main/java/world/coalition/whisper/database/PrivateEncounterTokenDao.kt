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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import world.coalition.whisper.GeoToken

/**
 * @author Lucien Loiseau on 04/04/20.
 */
@Dao
interface PrivateEncounterTokenDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(set: PrivateEncounterToken): Long

    @Query("SELECT COUNT(*) FROM privateencountertoken WHERE last_seen > :since")
    fun getCount(since: Long): Long

    @Query("SELECT COUNT(*) FROM privateencountertoken WHERE last_seen > :since AND  tag = :tag")
    fun getCountWithTag(tag: String, since: Long): Long

    @Query("SELECT * FROM privateencountertoken ORDER BY last_seen")
    fun getAll(): List<PrivateEncounterToken>

    @Query("SELECT * FROM privateencountertoken WHERE last_seen > :timeEpochMillis")
    fun getAllSince(timeEpochMillis: Long): List<PrivateEncounterToken>

    @Query("SELECT row_id FROM privateencountertoken WHERE tell_token = :telltoken")
    fun getRowIdForTellToken(telltoken: String): Long?

    @Query("SELECT row_id FROM privateencountertoken WHERE hear_token = :heartoken")
    fun getRowIdForHearToken(heartoken: String): Long?

    @Query("SELECT tell_token as token, geo_hash as geohash FROM privateencountertoken WHERE last_seen > :timeEpochMillis AND shared = 0 ORDER BY last_seen DESC")
    fun getAllRemainingTellTokenSince(timeEpochMillis: Long): List<GeoToken>

    @Query("SELECT tell_token as token, geo_hash as geohash FROM privateencountertoken WHERE last_seen > :timeEpochMillis AND shared = 0 ORDER BY last_seen DESC LIMIT :limit")
    fun getRemainingTellTokenSince(timeEpochMillis: Long, limit: Long): List<GeoToken>

    @Query("SELECT hear_token as token, geo_hash as geohash FROM privateencountertoken WHERE last_seen > :timeEpochMillis")
    fun getAllHearTokenSince(timeEpochMillis: Long): List<GeoToken>

    @Query("UPDATE privateencountertoken SET last_seen = :lastSeen WHERE row_id = :rowid")
    fun updateLastSeen(rowid: Long,  lastSeen: Long)

    @Query("UPDATE privateencountertoken SET tag = :tag WHERE hear_token = :heartoken")
    fun updateTag(heartoken: String, tag: String)

    @Query("UPDATE privateencountertoken SET shared = :shared WHERE tell_token = :telltoken")
    fun updateSharedStatus(telltoken: String, shared: Boolean)

    @Query("DELETE FROM privateencountertoken WHERE last_seen < :olderThan")
    fun pruneOldData(olderThan: Long): Int

    @Query("SELECT COUNT(ping.elapsed_time_duration) FROM blepingevent ping " +
            "INNER JOIN privateencountertoken pet ON pet.row_id = ping.pet_rowid " +
            "WHERE pet.tag = :tag AND ping.ping_timestamp_ms > :since")
    suspend fun estimateRiskExposure(tag: String, since: Long): Int
}