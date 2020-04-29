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
 * @author Lucien Loiseau on 08/04/20.
 */
@Dao
interface LocationUpdateDao {
    @Query("SELECT * FROM locationupdate")
    fun getAll(): List<LocationUpdate>

    @Insert
    fun insert(location: LocationUpdate): Long

    @Query("DELETE FROM locationupdate WHERE timestamp < :olderThan")
    fun pruneOldData(olderThan: Long): Int

    @Query("SELECT min(latitude) as minLatitude, min(longitude) as minLongitude, max(latitude) as maxLatitude, max(longitude) as maxLongitude FROM locationupdate WHERE timestamp >= :period")
    fun boundingBox(period: Long): BoundingBox?
}