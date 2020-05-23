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
import androidx.room.Query

/**
 * @author Lucien Loiseau on 07/04/20.
 */
@Dao
interface BlePingEventDao {
    @Query("SELECT * FROM blepingevent")
    fun getAll(): List<BlePingEvent>

    @Query("SELECT * FROM blepingevent WHERE pet_rowid = :petRowId ORDER BY ping_timestamp_ms DESC LIMIT 1")
    fun getLast(petRowId: Long): BlePingEvent?

    @Insert
    fun insert(cr: BlePingEvent): Long
}