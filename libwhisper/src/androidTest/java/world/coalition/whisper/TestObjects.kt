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

package world.coalition.whisper

import android.util.Base64
import org.bouncycastle.jce.interfaces.ECPublicKey
import world.coalition.whisper.database.BleConnectEvent
import world.coalition.whisper.id.ECUtil
import world.coalition.whisper.id.KeyPairParam
import world.coalition.whisper.id.ProofOfInteraction

/**
 * @author Lucien Loiseau on 04/04/20.
 */
object TestObjects {
    val SKA = KeyPairParam(ECUtil.generateKeyPair(),0,1000)
    val SKB = KeyPairParam(ECUtil.generateKeyPair(),1000,1000)
    val SKC = KeyPairParam(ECUtil.generateKeyPair(),2000,1000)
    val SKD = KeyPairParam(ECUtil.generateKeyPair(),3000,1000)
    val SKE = KeyPairParam(ECUtil.generateKeyPair(),4000,1000)

    val addra: String = "aa:aa:aa:aa:aa:aa"
    val addrb: String = "bb:bb:bb:bb:bb:bb"
    val addrc: String = "cc:cc:cc:cc:cc:cc"
    val addrd: String = "dd:dd:dd:dd:dd:dd"

    fun interaction(pub:ByteArray, peripheral: String, time: Long): BleConnectEvent {
        return BleConnectEvent(true, peripheral, time, 0x01, 1, pub, 0)
    }

    fun poi(local: KeyPairParam, peer: KeyPairParam): ProofOfInteraction {
        return ECUtil.getInteraction(local.keyPair, peer.publicKeyRaw())
    }

    fun ByteArray.toHex() =
        this.joinToString(separator = "") { it.toInt().and(0xff).toString(16) }

    fun dump() {
        val a = SKA.privateKeyRaw()
        val b = SKA.publicKeyRaw()

        println("SKA private b64: "+Base64.encodeToString(SKA.privateKeyRaw(), Base64.NO_WRAP))
        println("SKA private hex: "+SKA.privateKeyRaw().toHex())
        println("SKA public b64: "+Base64.encodeToString(SKA.publicKeyRaw(), Base64.NO_WRAP))
        println("SKA public hex: "+SKA.publicKeyRaw().toHex())
        println("SKB private b64: "+Base64.encodeToString(SKB.privateKeyRaw(), Base64.NO_WRAP))
        println("SKB private hex: "+SKB.privateKeyRaw().toHex())
        println("SKB public b64: "+Base64.encodeToString(SKB.publicKeyRaw(), Base64.NO_WRAP))
        println("SKB public hex: "+SKB.publicKeyRaw().toHex())
        println("SKC private b64: "+Base64.encodeToString(SKC.privateKeyRaw(), Base64.NO_WRAP))
        println("SKC private hex: "+SKC.privateKeyRaw().toHex())
        println("SKC public b64: "+Base64.encodeToString(SKC.publicKeyRaw(), Base64.NO_WRAP))
        println("SKC public hex: "+SKC.publicKeyRaw().toHex())
        println("SKD private b64: "+Base64.encodeToString(SKD.privateKeyRaw(), Base64.NO_WRAP))
        println("SKD private hex: "+SKD.privateKeyRaw().toHex())
        println("SKD public b64: "+Base64.encodeToString(SKD.publicKeyRaw(), Base64.NO_WRAP))
        println("SKD public hex: "+SKD.publicKeyRaw().toHex())
        println("SKE private b64: "+Base64.encodeToString(SKE.privateKeyRaw(), Base64.NO_WRAP))
        println("SKE private hex: "+SKE.privateKeyRaw().toHex())
        println("SKE public b64: "+Base64.encodeToString(SKE.publicKeyRaw(), Base64.NO_WRAP))
        println("SKE public hex: "+SKE.publicKeyRaw().toHex())

        val proofAB = ECUtil.doECDH(
            SKA.keyPair,
            SKB.publicKeyRaw()
        )
        println("shared secret A <> B b64: "+Base64.encodeToString(proofAB, Base64.NO_WRAP))
        println("shared secret A <> B hex: "+proofAB.toHex())

        val interactionAB = ECUtil.getInteraction(
            SKA.keyPair,
            SKB.publicKeyRaw()
        )
        println("interaction A <> B b64: hear="+Base64.encodeToString(interactionAB.hearToken, Base64.NO_WRAP)+" tell="+Base64.encodeToString(interactionAB.tellToken, Base64.NO_WRAP))
        println("interaction A <> B hex: hear="+interactionAB.hearToken.toHex()+" tell="+interactionAB.tellToken.toHex())

        val proofCD = ECUtil.doECDH(
            SKC.keyPair,
            SKD.publicKeyRaw()
        )
        println("shared secret C <> D: "+Base64.encodeToString(proofCD, Base64.NO_WRAP))
        println("shared secret C <> D hex: "+proofCD.toHex())
        val interactionCD = ECUtil.getInteraction(
            SKC.keyPair,
            SKD.publicKeyRaw()
        )
        println("interaction C <> D b64: hear="+Base64.encodeToString(interactionCD.hearToken, Base64.NO_WRAP)+" tell="+Base64.encodeToString(interactionCD.tellToken, Base64.NO_WRAP))
        println("interaction C <> D hex: hear="+interactionCD.hearToken.toHex()+" tell="+interactionCD.tellToken.toHex())

        val proofBE = ECUtil.doECDH(
            SKB.keyPair,
            SKE.publicKeyRaw()
        )
        println("shared secret B <> E: "+Base64.encodeToString(proofBE, Base64.NO_WRAP))
        println("shared secret B <> E hex: "+proofBE.toHex())

        val interactionBE = ECUtil.getInteraction(
            SKB.keyPair,
            SKE.publicKeyRaw()
        )
        println("interaction B <> E b64: hear="+Base64.encodeToString(interactionBE.hearToken, Base64.NO_WRAP)+" tell="+Base64.encodeToString(interactionBE.tellToken, Base64.NO_WRAP))
        println("interaction B <> E hex: hear="+interactionBE.hearToken.toHex()+" tell="+interactionBE.tellToken.toHex())


    }
}