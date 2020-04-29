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

import org.bouncycastle.crypto.digests.Blake2bDigest

/**
 * @author Lucien Loiseau on 06/04/20.
 */
class SecureTidBlake2b(hashId: ByteArray, challenge: ByteArray, hmac: ByteArray): SecureTid(hashId, challenge, hmac) {
    override fun validate(key: ByteArray): Boolean {
        val digest = Blake2bDigest(key, 16, null, null)
        val mac = ByteArray(digest.digestSize)
        digest.update(hashId, 0, hashId.size)
        digest.update(challenge, 0, challenge.size)
        digest.doFinal(mac, 0);
        return mac.contentEquals(hmac)
    }
}