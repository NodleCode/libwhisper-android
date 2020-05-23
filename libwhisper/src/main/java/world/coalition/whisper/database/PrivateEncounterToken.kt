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
 * @author Lucien Loiseau on 04/04/20.
 */
@Entity(
    indices = [
        Index(
            value = ["tell_token"],
            unique = true
        ),
        Index(
            value = ["hear_token"],
            unique = true
        )]
)
data class PrivateEncounterToken(
    @ColumnInfo(name = "tell_token") val tellToken: String,
    @ColumnInfo(name = "hear_token") val hearToken: String,
    @ColumnInfo(name = "geo_hash")   var geohash: String,
    @ColumnInfo(name = "last_seen")  val seen: Long,
    @ColumnInfo(name = "shared") val shared: Boolean,
    @ColumnInfo(name = "tag") val tag: String
) {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id", index = true)
    var id: Long = 0
}