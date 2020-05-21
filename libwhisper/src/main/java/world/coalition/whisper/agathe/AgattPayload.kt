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

package world.coalition.whisper.agathe

import kotlinx.serialization.Serializable

/**
 * @author Lucien Loiseau on 30/03/20.
 *
 * The payload as advertised by the device over BLE
 */
@Serializable
data class AgattPayload(
    val version: Int,
    val organization: Int,
    val pubKey: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AgattPayload

        if (version != other.version) return false
        if (organization != other.organization) return false
        if (!pubKey.contentEquals(other.pubKey)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + organization
        result = 31 * result + pubKey.contentHashCode()
        return result
    }
}