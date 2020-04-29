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

/**
 * interface TidGenerator provides helper function to generate Temporary Identifiers
 */
interface TidGenerator {

    /**
     * return true if this generator has expired, false otherwise
     */
    fun isExpire(timestampSec: Long): Boolean

    /**
     * return the secret backing this generator
     */
    fun getSecretParam(): SessionKeyParam

    /**
     * generateNthTid
     * @param n specify to use the n-th temporary Id
     * @return a temporary ID based on current Timestamp
     */
    fun generateNthTid(n: Long): ByteArray

    /**
     * generateNthTidWithChallenge
     * @param n specify to use the n-th temporary Id
     * @param challenge If not null it is added to the digest for the hash-based MAC
     * @return a secure ID
     */
    fun generateNthTidWithChallenge(keyId: Long, challenge: ByteArray): SecureTid

    /**
     * generateTid
     * @param timestamp choose which subkey to use based on timestamp with regards to time reference
     * @return the temporary ID at the given timestamp
     */
    fun generateTid(timestamp: Long): ByteArray

    /**
     * generateTid
     * @return the temporary ID based on current Timestamp
     */
    fun generateTid(): ByteArray

    /**
     * generateTidBase64
     * @param timestamp choose which subkey to use based on timestamp with regards to time reference
     * @return a temporary ID for the given timestamp
     */
    fun generateTidBase64(timestamp: Long): String

    /**
     * generateTidBase64
     * @return a temporary ID based on current Timestamp
     */
    fun generateTidBase64(): String

    /**
     * generateTidWithChallenge
     * @param timestamp choose which subkey to use based on timestamp with regards to time reference
     * @param challenge If not null it is added to the digest for the hash-based MAC,
     *        otherwise
     * @return a secure ID for the given timestamp
     */
    fun generateTidWithChallenge(timestamp: Long, challenge: ByteArray): SecureTid

    /**
     * generateTidWithChallenge
     * @param challenge or salt. If not null it is added to the digest for the hash-based MAC,
     *        otherwise it is ignored
     * @return a secure ID based on current Timestamp
     */
    fun generateTidWithChallenge(challenge: ByteArray): SecureTid
}
