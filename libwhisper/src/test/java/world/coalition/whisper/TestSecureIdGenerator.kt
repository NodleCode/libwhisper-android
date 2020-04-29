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

import org.junit.Test

class TestSecureIdGenerator {

    val challenge = byteArrayOf(0x00, 0x01, 0x02, 0x03)

    @Test
    fun testIdGeneration() {
        val sessionKey = world.coalition.whisper.id.TidGeneratorBlake2B.New()
        for(i in 0..10) {
            val t = sessionKey.sessionKey.TimeReferenceSec + sessionKey.sessionKey.TimeStepSec*i
            println("${sessionKey.sessionKey.TimeReferenceSec}  $t")
            val sidt = sessionKey.generateTidWithChallenge(t, challenge)
            assert(sidt.validate(sessionKey.sessionKey.SecretKey))
            val sidc = sessionKey.generateNthTidWithChallenge(i.toLong(), challenge)
            assert(sidc.validate(sessionKey.sessionKey.SecretKey))
            assert(sidt == sidc)
        }
    }

}
