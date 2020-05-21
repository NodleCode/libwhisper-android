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
import java.security.KeyPair

/**
 * @author Lucien Loiseau on 28/03/20.
 */
data class KeyPairParam(
    val keyPair: KeyPair,
    val TimeReferenceSec: Long,
    val ExpireAfterSec: Int
) {
    constructor(
        privateKeyRaw: ByteArray,
        publicKeyRaw: ByteArray,
        timeReferenceSec: Long,
        expireAfterSec: Int
    ) : this(
        KeyPair(
            ECUtil.loadPublicKey(publicKeyRaw),
            ECUtil.loadPrivateKey(privateKeyRaw)
        ),
        timeReferenceSec,
        expireAfterSec)

    override fun toString(): String {
        return "pubkey=${Base64.encodeToString(publicKeyRaw(), Base64.NO_WRAP)} ref=$TimeReferenceSec expire=$ExpireAfterSec"
    }

    fun publicKeyRaw(): ByteArray {
        return ECUtil.savePublicKey(keyPair.public)
    }

    fun privateKeyRaw(): ByteArray {
        return ECUtil.savePrivateKey(keyPair.private)
    }

    fun isExpire(time: Long): Boolean {
        return time > (TimeReferenceSec + ExpireAfterSec)
    }
}