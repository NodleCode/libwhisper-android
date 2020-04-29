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

import androidx.annotation.NonNull
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * @author Lucien Loiseau on 03/04/20.
 */
@Entity
class WhisperEvent(
    @ColumnInfo(name = "timestamp") var timestamp: Long, // event timestamp
    @ColumnInfo(name = "code")      var code: Int,
    @ColumnInfo(name = "int1")      var int1: Int,
    @ColumnInfo(name = "int2")      var int2: Int,
    @ColumnInfo(name = "str1")      var str1: String
) {
    @NonNull
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "row_id", index = true) var id: Long = 0
}

enum class EventCode(val code: Int) {
    SCAN_STARTED(0x00),
    SCAN_STOPPED(0x01),
    PROCESS_KEYS_START(0x02),
    PROCESS_KEYS_STOP(0x03)
}