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
import org.hamcrest.core.IsNot
import org.hamcrest.core.IsNull
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import world.coalition.whisper.database.WhisperDatabase
import world.coalition.whisper.id.ECUtil

/**
 * @author Lucien Loiseau on 05/04/20.
 */

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestKeyPair {
    private var db = WhisperDatabase.memory(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
    }

    @Test
    fun stage1_testSessionKeyInsertion() {
        // db should be empty
        runBlocking {
            val n = db.roomDb.userKeyPairDao().getAll()
            assertThat(n.size, equalTo(0))
        }

        // this one should create a new key
        val kp1 = db.getCurrentKeyPair(10, 100)
        val k1 =  Base64.encodeToString(ECUtil.savePrivateKey(kp1.private), Base64.NO_WRAP) + Base64.encodeToString(ECUtil.savePublicKey(kp1.public), Base64.NO_WRAP)


        // this one should return previous because not expire
        val kp2 = db.getCurrentKeyPair(20, 100)
        val k2 =  Base64.encodeToString(ECUtil.savePrivateKey(kp2.private), Base64.NO_WRAP) + Base64.encodeToString(ECUtil.savePublicKey(kp2.public), Base64.NO_WRAP)
        assertThat(k2, equalTo(k1))

        // this one should create a new key because previous expired (120 > 10 + 100)
        val kp3 = db.getCurrentKeyPair(130, 100)
        val k3 =  Base64.encodeToString(ECUtil.savePrivateKey(kp3.private), Base64.NO_WRAP) + Base64.encodeToString(ECUtil.savePublicKey(kp3.public), Base64.NO_WRAP)
        assertThat(k3, IsNot(equalTo(k2)))

        // this one should create a new key because previous expired (120 > 10 + 100)
        val kp4 = db.getCurrentKeyPair(240, 100)
        val k4 =  Base64.encodeToString(ECUtil.savePrivateKey(kp4.private), Base64.NO_WRAP) + Base64.encodeToString(ECUtil.savePublicKey(kp4.public), Base64.NO_WRAP)
        assertThat(k4, IsNot(equalTo(k3)))

        // this timestamp < current one so it matches
        val kp5 = db.getCurrentKeyPair(120, 100)
        val k5 =  Base64.encodeToString(ECUtil.savePrivateKey(kp5.private), Base64.NO_WRAP) + Base64.encodeToString(ECUtil.savePublicKey(kp5.public), Base64.NO_WRAP)
        assertThat(k5, equalTo(k4))

        runBlocking {
            val n = db.roomDb.userKeyPairDao().getAll()
            assertThat(n.size, equalTo(3))
            // descending order, most recent first
            val kn1 = n[0].prvKey+n[0].pubKey
            val kn2 = n[1].prvKey+n[1].pubKey
            val kn3 = n[2].prvKey+n[2].pubKey
            assertThat(kn1, equalTo(k4))
            assertThat(kn2, equalTo(k3))
            assertThat(kn3, equalTo(k1))
        }

        // get last 1 key should return gen5
        // should only have 1 left (gen4)
        runBlocking {
            val n = db.roomDb.userKeyPairDao().getLast()
            val klast = n!!.prvKey+n.pubKey
            assertThat(klast, equalTo(k4))
        }
    }

        @Test
    fun stage6_testTearDown() {
        db.roomDb.clearAllTables()
    }

    @Test
    fun stage7_closeDb() {
        db.close()
    }
}