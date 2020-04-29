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

import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.protobuf.ProtoBuf
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.serialization.UnstableDefault
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import world.coalition.whisper.id.TidGeneratorBlake2B
import world.coalition.whisper.TestObjects.challenge1
import world.coalition.whisper.agathe.TIDPayload

/**
 * @author Lucien Loiseau on 31/03/20.
 */
@RunWith(AndroidJUnit4::class)
class TestSerialization {

    @OptIn(UnstableDefault::class)
    @Test
    fun TestIdJson() {
        val gen = TidGeneratorBlake2B.New()
        val payloadIn = TIDPayload(
            1,
            0x01,
            gen.generateTidWithChallenge(challenge1)
        )
        val json = Json.stringify(TIDPayload.serializer(), payloadIn)
        println("size> "+json.length)
        val payloadOut = Json.parse(TIDPayload.serializer(), json)
        assertThat(payloadIn, equalTo(payloadOut))
    }

    @Test
    fun TestIdCbor() {
        val gen = TidGeneratorBlake2B.New()
        val payloadIn = TIDPayload(
            1,
            0x01,
            gen.generateTidWithChallenge(challenge1)
        )
        val cbor = Cbor.dump(TIDPayload.serializer(), payloadIn)
        println("size> "+cbor.size)
        val payloadOut = Cbor.load(TIDPayload.serializer(), cbor)
        assertThat(payloadIn, equalTo(payloadOut))
    }

    @Test
    fun TestIdProtobuf() {
        val gen = TidGeneratorBlake2B.New()
        val payloadIn = TIDPayload(
            1,
            0x01,
            gen.generateTidWithChallenge(challenge1)
        )
        val pb = ProtoBuf.dump(TIDPayload.serializer(), payloadIn)
        println("size> "+pb.size)
        val payloadOut = ProtoBuf.load(TIDPayload.serializer(), pb)
        assertThat(payloadIn, equalTo(payloadOut))
    }
}