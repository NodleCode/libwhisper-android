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

package world.coalition.whisper.id

import android.util.Base64

/**
 * A SecureID is a temporary ID derived from a SecretParam and contains three field:
 *
 * - hashid the temporary ID
 * - challenge or salt or anything that is associated with the hashid
 * - Hmac ( hashid + challenge )
 *
 * @author Lucien Loiseau on 28/03/20.
 */
abstract class SecureTid(val hashId: ByteArray, val challenge: ByteArray, val hmac: ByteArray) {

    abstract fun validate(key: ByteArray): Boolean

    fun hashIdBase64(): String {
        return Base64.encodeToString(hashId, Base64.NO_WRAP)
    }

    fun challengeIdBase64(): String {
        return Base64.encodeToString(challenge, Base64.NO_WRAP)
    }

    fun hmacBase64(): String {
        return Base64.encodeToString(hmac, Base64.NO_WRAP)
    }

    override fun toString(): String {
        val hash64 = Base64.encodeToString(hashId, Base64.NO_WRAP)
        val challenge64 = Base64.encodeToString(challenge, Base64.NO_WRAP)
        val hmac64 = Base64.encodeToString(hmac, Base64.NO_WRAP)

        return "hashid=${hash64} challenge=${challenge64}, hmac=${hmac64}"
    }

    // auto generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SecureTid

        if (!hashId.contentEquals(other.hashId)) return false
        if (!challenge.contentEquals(other.challenge)) return false
        if (!hmac.contentEquals(other.hmac)) return false

        return true
    }

    // auto generated
    override fun hashCode(): Int {
        var result = hashId.contentHashCode()
        result = 31 * result + challenge.contentHashCode()
        result = 31 * result + hmac.contentHashCode()
        return result
    }
}