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
import org.bouncycastle.crypto.digests.Blake2bDigest
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECParameterSpec
import org.bouncycastle.jce.spec.ECPrivateKeySpec
import org.bouncycastle.jce.spec.ECPublicKeySpec
import java.math.BigInteger
import java.security.*
import java.security.spec.ECGenParameterSpec
import javax.crypto.KeyAgreement

/**
 * @author Lucien Loiseau on 09/04/20.
 */
object ECUtil {

    private var kpgen: KeyPairGenerator

    init {
        Security.removeProvider (BouncyCastleProvider.PROVIDER_NAME);
        Security.insertProviderAt(BouncyCastleProvider(), 1);
        //Security.addProvider(BouncyCastleProvider())
        kpgen = KeyPairGenerator.getInstance("ECDH", "BC")
        kpgen.initialize(ECGenParameterSpec("curve25519"), SecureRandom())

    }

    fun generateKeyPair(): KeyPair {
        return kpgen.generateKeyPair()
    }

    fun getInteraction(keyPair: KeyPair, peerPubKey: ByteArray): ProofOfInteraction {
        // extract private and public keys
        val ecPrvKey: ECPrivateKey = keyPair.private as ECPrivateKey
        val ecPubkey: ECPublicKey = keyPair.public as ECPublicKey

        // compute shared secret with diffie-hellman
        val sharedSecret = doEllipticCurveDiffieHellman(ecPrvKey.d.toByteArray(), peerPubKey)

        // interaction tokens
        val localToken = doBlake2b(sharedSecret, ecPubkey.q.getEncoded(true))
        val peerToken = doBlake2b(sharedSecret, peerPubKey)
        return ProofOfInteraction(localToken, peerToken)
    }

    fun doECDH(keyPair: KeyPair, peerPubKey: ByteArray): ByteArray {
        val ecPrvKey: ECPrivateKey = keyPair.private as ECPrivateKey
        return doEllipticCurveDiffieHellman(ecPrvKey.d.toByteArray(), peerPubKey)
    }

    fun doEllipticCurveDiffieHellman(
        dataPrv: ByteArray,
        dataPub: ByteArray
    ): ByteArray {
        val ka: KeyAgreement = KeyAgreement.getInstance("ECDH", "BC")
        ka.init(loadPrivateKey(dataPrv))
        ka.doPhase(loadPublicKey(dataPub), true)
        return ka.generateSecret()
    }

    fun savePublicKey(key: PublicKey): ByteArray {
        val eckey: ECPublicKey = key as ECPublicKey
        return eckey.q.getEncoded(true)
    }

    fun savePublicKeyBase64(key: PublicKey): String {
        val raw = savePublicKey(key)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun loadPublicKey(data: ByteArray): PublicKey {
        val params: ECParameterSpec = ECNamedCurveTable.getParameterSpec("curve25519")
        val pubKey = ECPublicKeySpec(
            params.curve.decodePoint(data), params
        )
        val kf: KeyFactory = KeyFactory.getInstance("ECDH", "BC")
        return kf.generatePublic(pubKey)
    }

    fun savePrivateKey(key: PrivateKey): ByteArray {
        val eckey: ECPrivateKey = key as ECPrivateKey
        return eckey.d.toByteArray()
    }

    fun savePrivateKeyBase64(key: PrivateKey): String {
        val raw = savePrivateKey(key)
        return Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    fun loadPrivateKey(data: ByteArray): PrivateKey {
        val params: ECParameterSpec = ECNamedCurveTable.getParameterSpec("curve25519")
        val prvkey = ECPrivateKeySpec(BigInteger(data), params)
        val kf: KeyFactory = KeyFactory.getInstance("ECDH", "BC")
        return kf.generatePrivate(prvkey)
    }

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