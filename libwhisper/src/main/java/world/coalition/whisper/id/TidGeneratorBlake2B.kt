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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import org.bouncycastle.crypto.digests.Blake2bDigest
import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * @author Lucien Loiseau on 28/03/20.
 */
class TidGeneratorBlake2B(val sessionKey: SessionKeyParam) :
    TidGenerator {
    companion object {
        const val DEFAULT_SESSION_KEY_VALIDITY_SEC = 3600*24*7 // one week
        const val DEFAULT_TEMPORARY_KEY_VALIDITY_SEC = 3600   // one hour
        const val BLAKE2B_HASH_ID_SIZE = 20


        val random = SecureRandom()

        /*
         * return a new generator using the current timestamp as reference and a 1 hour timestep
         */
        fun New() = New(
            System.currentTimeMillis() / 1000,
            DEFAULT_SESSION_KEY_VALIDITY_SEC,
            DEFAULT_TEMPORARY_KEY_VALIDITY_SEC
        )

        /*
        * return a new Blake2bGenerator
        */
        fun New(timeReferenceSec: Long, expireAfterSec: Int, timeStepSec: Int): TidGeneratorBlake2B {
            val master = ByteArray(32)
            random.nextBytes(master)
            return TidGeneratorBlake2B(
                SessionKeyParam(
                    master,
                    timeReferenceSec,
                    expireAfterSec,
                    timeStepSec,
                    KdfAlgorithm.BLAKE2B160.code
                )
            )
        }

        private fun keyIdToSalt(c: Long) = ByteBuffer.allocate(16).putLong(c).array()
    }

    override fun isExpire(timestampSec: Long): Boolean {
        return timestampSec > sessionKey.TimeReferenceSec+sessionKey.ExpireAfterSec
    }

    override fun getSecretParam(): SessionKeyParam {
        return sessionKey
    }

    override fun generateNthTidWithChallenge(keyId: Long, challenge: ByteArray): SecureTid {
        // first derive a hashid from the session key
        val key = sessionKey.SecretKey
        val dkf = Blake2bDigest(key,
            BLAKE2B_HASH_ID_SIZE,
            keyIdToSalt(
                keyId
            ), "~WhisperSecureId".toByteArray() )
        val hashId = ByteArray(dkf.digestSize)
        dkf.doFinal(hashId, 0);

        // digest a mac
        val digest = Blake2bDigest(key, 16, null, null)
        val hmac = ByteArray(digest.digestSize)
        digest.update(hashId, 0, hashId.size)
        digest.update(challenge, 0, challenge.size)
        digest.doFinal(hmac, 0);
        return SecureTidBlake2b(
            hashId,
            challenge,
            hmac
        )
    }

    override fun generateTidWithChallenge(timestamp: Long, challenge: ByteArray): SecureTid {
        var keyId = ((timestamp - sessionKey.TimeReferenceSec) / sessionKey.TimeStepSec)
        if (keyId < 0) {
            keyId = 0
        }
        return generateNthTidWithChallenge(keyId, challenge)
    }

    override fun generateTidWithChallenge(challenge: ByteArray) = generateTidWithChallenge(System.currentTimeMillis()/1000, challenge)

    override fun generateNthTid(keyId: Long): ByteArray {
        val sid = generateNthTidWithChallenge(keyId, byteArrayOf(0x00))
        return sid.hashId
    }

    override fun generateTid(timestamp: Long): ByteArray {
        val sid = generateTidWithChallenge(timestamp, byteArrayOf(0x00))
        return sid.hashId
    }

    override fun generateTid(): ByteArray = generateTid(System.currentTimeMillis()/1000)

    override fun generateTidBase64(timestamp: Long): String {
        val tid = generateTid(System.currentTimeMillis()/1000)
        return Base64.encodeToString(tid, Base64.NO_WRAP)
    }

    override fun generateTidBase64(): String = generateTidBase64(System.currentTimeMillis()/1000)

    //@OptIn(UnstableDefault::class)
    override fun toString(): String {
        return Json(JsonConfiguration.Default).stringify(SessionKeyParam.serializer(), sessionKey)
    }
}