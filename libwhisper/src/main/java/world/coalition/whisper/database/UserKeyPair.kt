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

import android.util.Base64
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import world.coalition.whisper.id.KeyPairParam

/**
 * @author Lucien Loiseau on 29/03/20.
 */
@Entity(
    indices = [
        Index(
            value = ["public_key"],
            unique = true
        )]
)
data class UserKeyPair(
    @ColumnInfo(name = "private_key") val prvKey: String,
    @ColumnInfo(name = "public_key") val pubKey: String,
    @ColumnInfo(name = "time_reference") val tr: Long,
    @ColumnInfo(name = "expiry_after_sec") val exp: Int
) {
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "row_id", index = true) var row_id: Long = 0

    constructor(param: KeyPairParam) : this(
        Base64.encodeToString(param.privateKeyRaw(), Base64.NO_WRAP),
        Base64.encodeToString(param.publicKeyRaw(), Base64.NO_WRAP),
        param.TimeReferenceSec,
        param.ExpireAfterSec
    )

    fun toKeyPairParam(): KeyPairParam {
        return KeyPairParam(
            Base64.decode(prvKey,Base64.NO_WRAP),
            Base64.decode(pubKey,Base64.NO_WRAP),
            tr,
            exp)
    }
}