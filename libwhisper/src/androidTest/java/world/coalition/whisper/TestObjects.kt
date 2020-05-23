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
}