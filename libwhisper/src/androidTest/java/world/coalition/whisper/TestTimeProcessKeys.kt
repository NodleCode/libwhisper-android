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
import org.hamcrest.CoreMatchers
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import world.coalition.whisper.id.SessionKeyParam
import world.coalition.whisper.id.TidGeneratorBlake2B
import world.coalition.whisper.database.PeerContactEvent
import world.coalition.whisper.database.WhisperDatabase
import java.security.SecureRandom

/**
 * @author Lucien Loiseau on 05/04/20.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestTimeProcessKeys {

    private var db = WhisperDatabase.persistent(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
        Thread.sleep(500)
    }

    @Test
    fun stage1_feedTable() {
        val random = SecureRandom()
        val SKA = TidGeneratorBlake2B.New(0,1000000,1)
        //val start = System.currentTimeMillis()
        for(i in 0..1000) {
            //println(">>>>>> generate contact:  $i")
            // generate contact
            val challenge = ByteArray(5)
            random.nextBytes(challenge)
            val tid = SKA.generateNthTidWithChallenge(i.toLong(), challenge)
            val peripheral: String = Base64.encodeToString(byteArrayOf((i%100).toByte()), Base64.NO_WRAP)
            val c = PeerContactEvent(peripheral, i.toLong(), "test", 1, tid.hashIdBase64(), tid.challengeIdBase64(), tid.hmacBase64(), -1*(i%100), "")
            db.addContact(c)
        }
        //val end = System.currentTimeMillis()
        //println(">>>>>>>>>>: time to feed 1000 keys: ${end - start}")
        val contactDumps = db.roomDb.peerContactEventDao().getAll()
        assertThat(contactDumps.size, CoreMatchers.equalTo(1001))
    }

    @Test
    fun stage2_process100keys() {
        val infected = mutableListOf<SessionKeyParam>()
        for (i in 0..100) {
            //println(">>>>>  creating sk: $i")
            infected.add(TidGeneratorBlake2B.New(i.toLong(), 100000,1).sessionKey)
        }

        val start = System.currentTimeMillis()
        runBlocking {
            db.processInfectedKeys(infected,"COID19",24*14)
        }
        val end = System.currentTimeMillis()
        println(">>>>>>>>>>: time to query 100 keys: ${end - start}")
    }


}