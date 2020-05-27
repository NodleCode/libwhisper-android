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
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.hasItem
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import world.coalition.whisper.TestObjects.SKA
import world.coalition.whisper.TestObjects.SKB
import world.coalition.whisper.TestObjects.SKC
import world.coalition.whisper.TestObjects.SKD
import world.coalition.whisper.TestObjects.SKE
import world.coalition.whisper.TestObjects.addra
import world.coalition.whisper.TestObjects.addrb
import world.coalition.whisper.TestObjects.addrc
import world.coalition.whisper.TestObjects.addrd
import world.coalition.whisper.TestObjects.interaction
import world.coalition.whisper.TestObjects.poi
import world.coalition.whisper.database.*
import world.coalition.whisper.database.PrivateEncounterToken
import world.coalition.whisper.id.ECUtil

/**
 * @author Lucien Loiseau on 04/04/20.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestBleConnectEventDb {
    private var db = WhisperDatabase.persistent(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
        Thread.sleep(500)
    }

    @Test
    fun stage1_testKeyPairInsertion() {
        runBlocking {
            var kps = db.roomDb.userKeyPairDao().getAll()
            assertThat(kps.size, equalTo(0))
            db.roomDb.userKeyPairDao().insert(UserKeyPair(SKA))
            kps = db.roomDb.userKeyPairDao().getAll()
            assertThat(kps.size, equalTo(1))
        }
    }

    @Test
    fun stage2_testInteractionInsertion() {
        var peerPingDumps = db.roomDb.blePingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(0))

        var interactionDumps = db.roomDb.bleConnectEventDao().getAll()
        assertThat(interactionDumps.size, equalTo(0))

        var peerPubKeyDumps = db.roomDb.privateEncounterTokenDao().getAll()
        assertThat(peerPubKeyDumps.size, equalTo(0))

        db.addInteraction(interaction(SKB.publicKeyRaw(), addra, 2020), poi(SKA, SKB), "") // new contact, new peripheral
        db.addInteraction(interaction(SKB.publicKeyRaw(), addrb, 2050), poi(SKA, SKB), "") // same contact different peripheral
        db.addInteraction(interaction(SKB.publicKeyRaw(), addrc, 2075), poi(SKA, SKB), "") // same contact, different tid and peripheral
        db.addInteraction(interaction(SKC.publicKeyRaw(), addrd,  2250), poi(SKA, SKC), "") // new contact, new peripheral
        db.addInteraction(interaction(SKD.publicKeyRaw(), addra,  3250), poi(SKA, SKD), "") // new contact, same peripheral seen before
        db.addPing(addra, 0, 3350,200)
        db.addPing(addra, 0, 3450, 200)
        db.addPing(addra, 0, 3550, 200)
        db.addPing(addra, 0, 4000, 200)

        // check if contact were added to the contact table
        interactionDumps = db.roomDb.bleConnectEventDao().getAll()
        assertThat(interactionDumps.size, equalTo(5))

        // check ping (5 ping from contact + 3)
        peerPingDumps = db.roomDb.blePingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(8))

        // check if peer were added to the peer table and updated
        peerPubKeyDumps = db.roomDb.privateEncounterTokenDao().getAll() // SKB SKC SKD
        assertThat(peerPubKeyDumps.size, equalTo(3))
        assertThat(peerPubKeyDumps[0].seen, equalTo(2075L))
        assertThat(peerPubKeyDumps[1].seen, equalTo(2250L))
        assertThat(peerPubKeyDumps[2].seen, equalTo(3250L))
    }

    @Test
    fun stage3_testPeerDetection() {
        // process infected keys
        val skbtoken = Base64.encodeToString(ECUtil.getInteraction(SKA.keyPair, SKB.publicKeyRaw()).hearToken, Base64.NO_WRAP)
        val skdtoken = Base64.encodeToString(ECUtil.getInteraction(SKA.keyPair, SKD.publicKeyRaw()).hearToken, Base64.NO_WRAP)
        val sketoken = Base64.encodeToString(ECUtil.getInteraction(SKA.keyPair, SKE.publicKeyRaw()).hearToken, Base64.NO_WRAP)


        var ret = db.processTellTokens(listOf(skbtoken), "SARS-Cov", 1000)
        assertThat(ret, equalTo(1))

        ret = db.processTellTokens(listOf(skdtoken), "covid-19", 1000)
        assertThat(ret, equalTo(1))

        ret = db.processTellTokens(listOf(sketoken), "SARS-Cov-2", 1000)
        assertThat(ret, equalTo(0))
    }


    @Test
    fun stage4_testRiskExposure() {
        var exp: Int = 0

        val total = db.getNumberOfInteractions(0)
        assertThat(total, equalTo(3L))
        val covid19 = db.getNumberOfRiskInteractions("covid-19",0)
        assertThat(covid19, equalTo(1L))
        val sars = db.getNumberOfRiskInteractions("SARS-Cov", 0)
        assertThat(sars, equalTo(1L))

        runBlocking {
            exp = db.getRiskExposure("covid-19", 0)
        }
        assertThat(exp, equalTo(4))

        runBlocking {
            exp = db.getRiskExposure("SARS-Cov", 0)
        }
        assertThat(exp, equalTo(3))
    }


    @Test
    fun stage5_testPubKeyDeletion() {
        // DELETE PEERS
        val deleted = db.roomDb.privateEncounterTokenDao().pruneOldData(2300)
        assertThat(deleted, equalTo(2)) // skb skc

        val peerIdDumps = db.roomDb.privateEncounterTokenDao().getAll() // skd
        assertThat(peerIdDumps.size, equalTo(1))

        // check that contact was deleted because of foreign key constraint
        val contactDumps = db.roomDb.bleConnectEventDao().getAll() // skd
        assertThat(contactDumps.size, equalTo(1))

        // peerPings should only have ping for skd
        val peerPingDumps = db.roomDb.blePingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(4))
    }

    @Test
    fun stage7_closeDb() {
        db.close()
    }
}