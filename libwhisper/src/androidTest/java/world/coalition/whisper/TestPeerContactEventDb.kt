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
import world.coalition.whisper.TestObjects.IDC1a
import world.coalition.whisper.TestObjects.IDC1b
import world.coalition.whisper.TestObjects.IDC2a
import world.coalition.whisper.TestObjects.IDD2a
import world.coalition.whisper.TestObjects.SKA
import world.coalition.whisper.TestObjects.SKB
import world.coalition.whisper.TestObjects.SKC
import world.coalition.whisper.TestObjects.SKD
import world.coalition.whisper.TestObjects.SKE
import world.coalition.whisper.TestObjects.addra
import world.coalition.whisper.TestObjects.addrb
import world.coalition.whisper.TestObjects.addrc
import world.coalition.whisper.TestObjects.addrd
import world.coalition.whisper.TestObjects.contact
import world.coalition.whisper.database.*

/**
 * @author Lucien Loiseau on 04/04/20.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestPeerContactEventDb {
    private var db = WhisperDatabase.persistent(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
        Thread.sleep(500)
    }

    @Test
    fun stage1_testSessionKeyInsertion() {
        runBlocking {
            var extracted = db.extractLastSessionKeys(0)
            assertThat(extracted.size, equalTo(0))

            val ska = SessionKey(SKA.sessionKey, true, WhisperDatabase.localTag)
            val skb = SessionKey(SKB.sessionKey, true, WhisperDatabase.localTag)
            db.roomDb.sessionKeyDao().insert(ska)
            db.roomDb.sessionKeyDao().insert(skb)

            extracted = db.extractLastSessionKeys(2)
            assertThat(extracted.size, equalTo(2))
            assertThat(extracted, hasItem(SKA.sessionKey))
            assertThat(extracted, hasItem(SKB.sessionKey))
        }
    }

    @Test
    fun stage2_testContactInsertion() {
        var peerIdDumps = db.roomDb.peerTidDao().getAll()
        assertThat(peerIdDumps.size, equalTo(0))

        var peerPingDumps = db.roomDb.peerPingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(0))

        var contactDumps = db.roomDb.peerContactEventDao().getAll()
        assertThat(contactDumps.size, equalTo(0))

        db.addContact(contact(IDC1a, addra, 2020)) // new contact, new peripheral
        db.addContact(contact(IDC1a, addrb, 2050)) // same contact different peripheral
        db.addContact(contact(IDC1b, addrc, 2075)) // same contact, different tid and peripheral
        db.addContact(contact(IDC2a, addrd, 2250)) // new contact, new peripheral
        db.addContact(contact(IDD2a, addra, 3250)) // new contact, same peripheral seen before
        db.addPing(addra, 3350,200)   // matches idd2a
        db.addPing(addra, 3450, 200)  // matches idd2a
        db.addPing(addra, 3550, 200)  // matches idd2a
        db.addPing(addra, 4000, 200)  // matches idd2a but elapsed time too big

        // check if contact were added to the contact table
        contactDumps = db.roomDb.peerContactEventDao().getAll()
        assertThat(contactDumps.size, equalTo(5))

        // check ping (5 ping from contact + 3)
        peerPingDumps = db.roomDb.peerPingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(8))

        // check if peer were added to the peer table and updated
        peerIdDumps = db.roomDb.peerTidDao().getAll() // IDC1 IDC3 IDD3 (IDC1 & IDC2 same)
        assertThat(peerIdDumps.size, equalTo(3))
        assertThat(peerIdDumps, hasItem(PeerTid(IDC1a.hashIdBase64(), 2075, db.unknownSessionKeyRowId))) // last seen
        assertThat(peerIdDumps, hasItem(PeerTid(IDC2a.hashIdBase64(), 2250, db.unknownSessionKeyRowId)))
        assertThat(peerIdDumps, hasItem(PeerTid(IDD2a.hashIdBase64(), 3250, db.unknownSessionKeyRowId)))
    }

    @Test
    fun stage3_testPeerDetection() {
        var skDumps = db.roomDb.sessionKeyDao().getAllLocal()
        assertThat(skDumps.size, equalTo(3)) // from stage1 + 1 default for unknown sk
        skDumps = db.roomDb.sessionKeyDao().getAllAlien()
        assertThat(skDumps.size, equalTo(0)) // from stage1

        // process infected keys
        var ret = db.processInfectedKeys(listOf(SKC.sessionKey), "SARS-Cov", 1000)
        assertThat(ret, equalTo(2)) // only IDC1 and IDC2

        ret = db.processInfectedKeys(listOf(SKD.sessionKey), "covid-19", 1000)
        assertThat(ret, equalTo(1)) // only IDD1

        ret = db.processInfectedKeys(listOf(SKE.sessionKey), "SARS-Cov-2", 1000)
        assertThat(ret, equalTo(0)) // none

        // check that sessions keys were added accordingly
        skDumps = db.roomDb.sessionKeyDao().getAllLocal()
        assertThat(skDumps.size, equalTo(3)) // should not have changed

        skDumps = db.roomDb.sessionKeyDao().getAllAlien()
        assertThat(skDumps.size, equalTo(2)) // SKC and SKD
        assertThat(skDumps, hasItem(SessionKey(SKC.sessionKey, false, "SARS-Cov")))
        assertThat(skDumps, hasItem(SessionKey(SKD.sessionKey, false, "covid-19")))

        val peerDumps = db.roomDb.peerTidDao().getAllTag("SARS-Cov")
        assertThat(peerDumps.size, equalTo(2)) // IDC1<>SKC IDC2<>SKC
    }

    @Test
    fun stage4_testPeerDetection() {
        var exp: Int = 0
        runBlocking {
            exp = db.getInfectionExposure("covid-19") // IDD2a +  3 pings
        }
        assertThat(exp, equalTo(4))

        runBlocking {
            exp = db.getInfectionExposure("SARS-Cov") // IDC1a IDC1a IDC1b IDC2a
        }
        assertThat(exp, equalTo(4))
    }


    @Test
    fun stage5_testPeerDeletion() {
        // DELETE PEERS
        val deleted = db.roomDb.peerTidDao().pruneOldData(2300)
        assertThat(deleted, equalTo(2))

        val peerIdDumps = db.roomDb.peerTidDao().getAll() // IDD3
        assertThat(peerIdDumps.size, equalTo(1))

        // check that contact was deleted because of foreign key constraint
        val contactDumps = db.roomDb.peerContactEventDao().getAll() // IDD2a , addra
        assertThat(contactDumps.size, equalTo(1))

        // peerPings should only have ping for IDD2a
        val peerPingDumps = db.roomDb.peerPingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(4))
    }

    @Test
    fun stage6_testSkDeletion() {
        // DELETE SESSIONKEYS
        db.roomDb.sessionKeyDao().delete(Base64.encodeToString(SKD.sessionKey.SecretKey, Base64.NO_WRAP))

        val peerIdDumps = db.roomDb.peerTidDao().getAll()
        assertThat(peerIdDumps.size, equalTo(0))

        // check that contact was deleted because of foreign key constraint
        val contactDumps = db.roomDb.peerContactEventDao().getAll()
        assertThat(contactDumps.size, equalTo(0))

        // check that pings was deleted because of foreign key constraint
        val peerPingDumps = db.roomDb.peerPingEventDao().getAll()
        assertThat(peerPingDumps.size, equalTo(0))
    }

    @Test
    fun stage7_closeDb() {
        db.close()
    }
}