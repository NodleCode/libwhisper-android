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

/**
 * @author Lucien Loiseau on 04/04/20.
 */
@Dao
interface PeerTidDao {
    @Query("SELECT * FROM peertid")
    fun getAll(): List<PeerTid>

    @Query("SELECT * FROM peertid peer" +
            " INNER JOIN sessionkey sk ON peer.session_key_row_id = sk.row_id " +
            " WHERE sk.tag = :tag")
    fun getAllTag(tag: String): List<PeerTid>

    @Query("SELECT row_id FROM peertid WHERE tid = :tid")
    fun getRowId(tid: String): Long?

    @Query("UPDATE peertid SET last_seen = :lastSeen WHERE tid = :tid")
    fun updateLastSeen(tid: String, lastSeen: Long)

    @Query("UPDATE peertid SET session_key_row_id = :skRowId WHERE tid = :tid")
    fun updateSessionKey(tid: String, skRowId: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(peerTid: PeerTid): Long

    @Query("DELETE FROM peertid WHERE last_seen < :olderThan")
    fun pruneOldData(olderThan: Long): Int
}