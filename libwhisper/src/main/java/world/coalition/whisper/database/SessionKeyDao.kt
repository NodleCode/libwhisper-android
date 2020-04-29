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

import androidx.room.*

/**
 * @author Lucien Loiseau on 29/03/20.
 */
@Dao
interface SessionKeyDao {
    @Query("SELECT * FROM sessionkey")
    fun getAll(): List<SessionKey>

    @Query("SELECT * FROM sessionkey WHERE is_local = 1")
    fun getAllLocal(): List<SessionKey>

    @Query("SELECT * FROM sessionkey WHERE is_local = 0")
    fun getAllAlien(): List<SessionKey>

    @Query("SELECT * FROM sessionkey WHERE session_key = :masterKey")
    fun get(masterKey: String): SessionKey

    @Query("SELECT * FROM sessionkey WHERE is_local = :local AND tag = :tag ORDER BY time_reference DESC LIMIT 1;")
    fun getLast(local: Int, tag: String): SessionKey?

    @Query("SELECT * FROM sessionkey WHERE is_local = :local AND tag = :tag ORDER BY time_reference DESC LIMIT :n;")
    suspend fun getLast(local: Int, tag: String, n: Int): List<SessionKey>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(user: SessionKey): Long

    @Query("DELETE FROM sessionkey WHERE session_key = :sessionKey")
    fun delete(sessionKey: String)

    @Query("SELECT COUNT(ping.elapsed_time_duration) FROM peerpingevent ping " +
            "INNER JOIN " +
                "(SELECT peer.row_id " +
                "FROM peertid peer " +
                "INNER JOIN sessionkey sk ON peer.session_key_row_id = sk.row_id " +
                "WHERE sk.tag = :tag) sub " +
            "WHERE sub.row_id = ping.peer_tid_rowid")
    suspend fun estimateExposure(tag: String): Int
}