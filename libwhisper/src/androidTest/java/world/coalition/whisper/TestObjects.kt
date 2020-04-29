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

import world.coalition.whisper.id.TidGeneratorBlake2B
import world.coalition.whisper.id.SecureTid
import world.coalition.whisper.database.PeerContactEvent

/**
 * @author Lucien Loiseau on 04/04/20.
 */
object TestObjects {

    val challenge1 = byteArrayOf(0x00, 0x01, 0x02, 0x03)
    val challenge2 = byteArrayOf(0x04, 0x05, 0x06, 0x07)

    val SKA = TidGeneratorBlake2B.New(0,1000,120)
    val IDA1a = SKA.generateTidWithChallenge(0,challenge1)
    val IDA1b = SKA.generateTidWithChallenge(70,challenge1)  // same hashid as ida1 different hmac
    val IDA2a = SKA.generateTidWithChallenge(200, challenge1) // different hashid, different hmac

    val SKB = TidGeneratorBlake2B.New(1000,1000,120)
    val IDB1a = SKB.generateTidWithChallenge(1000,challenge1)
    val IDB1b = SKB.generateTidWithChallenge(1070,challenge1)
    val IDB2a = SKB.generateTidWithChallenge(1200,challenge1)

    val SKC = TidGeneratorBlake2B.New(2000,1000,120)
    val IDC1a = SKC.generateTidWithChallenge(2000,challenge1)
    val IDC1b = SKC.generateTidWithChallenge(2070,challenge1)
    val IDC2a = SKC.generateTidWithChallenge(2200,challenge1)

    val SKD = TidGeneratorBlake2B.New(3000,1000,120)
    val IDD1a = SKD.generateTidWithChallenge(3000,challenge1)
    val IDD1b = SKD.generateTidWithChallenge(3070,challenge1)
    val IDD2a = SKD.generateTidWithChallenge(3200,challenge1)

    val SKE = TidGeneratorBlake2B.New(4000,1000,120)
    val IDE1a = SKE.generateTidWithChallenge(4000,challenge1)
    val IDE1b = SKE.generateTidWithChallenge(4070,challenge1)
    val IDE2a = SKE.generateTidWithChallenge(4200,challenge1)

    val addra: String = "aa:aa:aa:aa:aa:aa"
    val addrb: String = "bb:bb:bb:bb:bb:bb"
    val addrc: String = "cc:cc:cc:cc:cc:cc"
    val addrd: String = "dd:dd:dd:dd:dd:dd"
    val addre: String = "ee:ee:ee:ee:ee:ee"

    fun contact(id: SecureTid, addr: String, time: Long): PeerContactEvent {
        return PeerContactEvent(addr, time, "Coalition", 1, id.hashIdBase64(), id.challengeIdBase64(), id.hmacBase64(),0, "")
    }
}