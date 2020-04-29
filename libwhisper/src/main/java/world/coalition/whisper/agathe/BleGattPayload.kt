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
import world.coalition.whisper.id.SecureTid

/**
 * @author Lucien Loiseau on 30/03/20.
 *
 * The payload as advertised by the device over BLE
 */
@Serializable
data class TIDPayload(
    val version: Int,
    val organization: Int,
    val temporaryId: ByteArray,
    val challenge: ByteArray,
    val hmac: ByteArray
) {
    constructor(v: Int, org: Int, sid: SecureTid)
            : this(v, org, sid.hashId, sid.challenge, sid.hmac)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TIDPayload

        if (version != other.version) return false
        if (organization != other.organization) return false
        if (!temporaryId.contentEquals(other.temporaryId)) return false
        if (!challenge.contentEquals(other.challenge)) return false
        if (!hmac.contentEquals(other.hmac)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + organization
        result = 31 * result + temporaryId.contentHashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + hmac.contentHashCode()
        return result
    }


}

@Serializable
data class TIDWithChallengePayload(
    val version: Int,
    val organization: Int,
    val temporaryId: ByteArray,
    val challenge1: ByteArray,
    val hmac: ByteArray,
    val challenge2: ByteArray
) {
    constructor(v: Int, org: Int, sid: SecureTid, challenge: ByteArray) :
            this(v, org, sid.hashId, sid.challenge, sid.hmac, challenge)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as TIDWithChallengePayload

        if (version != other.version) return false
        if (organization != other.organization) return false
        if (temporaryId != other.temporaryId) return false
        if (!challenge1.contentEquals(other.challenge1)) return false
        if (!hmac.contentEquals(other.hmac)) return false
        if (!challenge2.contentEquals(other.challenge2)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = version
        result = 31 * result + organization
        result = 31 * result + temporaryId.hashCode()
        result = 31 * result + challenge1.contentHashCode()
        result = 31 * result + hmac.contentHashCode()
        result = 31 * result + challenge2.contentHashCode()
        return result
    }

}
