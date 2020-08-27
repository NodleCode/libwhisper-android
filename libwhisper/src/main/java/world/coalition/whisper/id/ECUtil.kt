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
import org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.bouncycastle.crypto.AsymmetricCipherKeyPairGenerator
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.crypto.generators.X25519KeyPairGenerator
import org.bouncycastle.crypto.params.X25519KeyGenerationParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import java.security.SecureRandom

/**
 * @author Lucien Loiseau on 09/04/20.
 */
object ECUtil {

    private var kpgen: AsymmetricCipherKeyPairGenerator = X25519KeyPairGenerator()

    init {
        kpgen.init(X25519KeyGenerationParameters(SecureRandom()))
    }

    fun generateKeyPair(): AsymmetricCipherKeyPair {
        return kpgen.generateKeyPair()
    }

    fun getInteraction(keyPair: AsymmetricCipherKeyPair, peerPubKey: ByteArray): ProofOfInteraction {
        // extract private and public keys
        val ecPrvKey: X25519PrivateKeyParameters = keyPair.private as X25519PrivateKeyParameters
        val ecPubkey: X25519PublicKeyParameters = keyPair.public as X25519PublicKeyParameters

        // compute shared secret with diffie-hellman
        val sharedSecret = doEllipticCurveDiffieHellman(ecPrvKey.encoded, peerPubKey)

        // interaction tokens
        val localToken = doBlake2b(sharedSecret, ecPubkey.encoded)
        val peerToken = doBlake2b(sharedSecret, peerPubKey)
        return ProofOfInteraction(localToken, peerToken)
    }

    fun doECDH(keyPair: AsymmetricCipherKeyPair, peerPubKey: ByteArray): ByteArray {
        val ecPrvKey: X25519PrivateKeyParameters = keyPair.private as X25519PrivateKeyParameters
        return doEllipticCurveDiffieHellman(ecPrvKey.encoded, peerPubKey)
    }

    fun doEllipticCurveDiffieHellman(
        dataPrv: ByteArray,
        dataPub: ByteArray
    ): ByteArray {
        val agree = X25519Agreement()
        agree.init(loadPrivateKey(dataPrv))
        val secret = ByteArray(agree.agreementSize)
        agree.calculateAgreement(loadPublicKey(dataPub), secret, 0)
        return secret
    }

    fun savePublicKey(key: X25519PublicKeyParameters): ByteArray {
        return key.encoded
    }

    fun savePublicKeyBase64(key: X25519PublicKeyParameters): String {
        val raw = savePublicKey(key)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun loadPublicKey(data: ByteArray): X25519PublicKeyParameters =
        X25519PublicKeyParameters(data, 0)

    fun savePrivateKey(key: X25519PrivateKeyParameters): ByteArray {
        return key.encoded
    }

    fun savePrivateKeyBase64(key: X25519PrivateKeyParameters): String {
        val raw = savePrivateKey(key)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun loadPrivateKey(data: ByteArray): X25519PrivateKeyParameters =
        X25519PrivateKeyParameters(data, 0)

    fun doBlake2b(
        K: ByteArray,
        dataPub: ByteArray
    ): ByteArray {
        val keyedHash = Blake2bDigest(160)
        val hash = ByteArray(keyedHash.digestSize)
        keyedHash.update(K, 0, K.size);
        keyedHash.update(dataPub, 0, dataPub.size);
        keyedHash.doFinal(hash, 0);
        return hash
    }
}