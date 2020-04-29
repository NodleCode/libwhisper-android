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

/**
 * @author Lucien Loiseau on 05/04/20.
 */

@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestSessionKeys {
    private var db = WhisperDatabase.memory(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
    }

    @Test
    fun stage1_testSessionKeyInsertion() {
        // db should be empty
        runBlocking {
            val n = db.extractLastSessionKeys(5)
            assertThat(n.size, equalTo(0))
        }

        // this one should create a new key
        val gen1 = db.getCurrentGenerator(10, 100, 10)
        val k1 = Base64.encodeToString(gen1.getSecretParam().SecretKey, Base64.NO_WRAP)

        // this one should return previous because not expire
        val gen2 = db.getCurrentGenerator(20, 100, 10)
        val k2 = Base64.encodeToString(gen2.getSecretParam().SecretKey, Base64.NO_WRAP)
        assertThat(k2, equalTo(k1))

        // this one should create a new key because previous expired (120 > 10 + 100)
        val gen3 = db.getCurrentGenerator(130, 100, 10)
        val k3 = Base64.encodeToString(gen3.getSecretParam().SecretKey, Base64.NO_WRAP)
        assertThat(k3, IsNot(equalTo(k2)))

        // this one should create a new key because previous expired (120 > 10 + 100)
        val gen4 = db.getCurrentGenerator(240, 100, 10)
        val k4 = Base64.encodeToString(gen4.getSecretParam().SecretKey, Base64.NO_WRAP)
        assertThat(k4, IsNot(equalTo(k3)))

        // this timestamp < current one so it matches
        val gen5 = db.getCurrentGenerator(120, 100, 10)
        val k5 = Base64.encodeToString(gen5.getSecretParam().SecretKey, Base64.NO_WRAP)
        assertThat(k5, equalTo(k4))

        runBlocking {
            val n = db.extractLastSessionKeys(5)
            assertThat(n.size, equalTo(3))
            // descending order, most recent first
            val kn1 = Base64.encodeToString(n[0].SecretKey, Base64.NO_WRAP)
            val kn2 = Base64.encodeToString(n[1].SecretKey, Base64.NO_WRAP)
            val kn3 = Base64.encodeToString(n[2].SecretKey, Base64.NO_WRAP)
            assertThat(kn1, equalTo(k4))
            assertThat(kn2, equalTo(k3))
            assertThat(kn3, equalTo(k1))
        }

        // get last 1 key should return gen5
        // should only have 1 left (gen4)
        runBlocking {
            val n = db.extractLastSessionKeys(1)
            assertThat(n.size, equalTo(1))
            val klast = Base64.encodeToString(n[0].SecretKey, Base64.NO_WRAP)
            assertThat(klast, equalTo(k4))
        }

        // we evict the last key
        db.evictLocalKeys(listOf(gen4.getSecretParam()))

        // should have 3 keys left because last one, which was active, was regenerated
        runBlocking {
            val n = db.extractLastSessionKeys(5)
            assertThat(n.size, equalTo(3))
            // descending order, most recent first
            val kn1 = Base64.encodeToString(n[0].SecretKey, Base64.NO_WRAP)
            val kn2 = Base64.encodeToString(n[1].SecretKey, Base64.NO_WRAP)
            val kn3 = Base64.encodeToString(n[2].SecretKey, Base64.NO_WRAP)
            assertThat(kn1, IsNot(equalTo(k4)))
            assertThat(kn1, IsNot(equalTo(k3)))
            assertThat(kn1, IsNot(equalTo(k1)))
            assertThat(kn2, equalTo(k3))
            assertThat(kn3, equalTo(k1))
        }

        // we evict the first two keys
        db.evictLocalKeys(listOf(gen1.getSecretParam(), gen3.getSecretParam()))

        // should only have 1 left (the new one)
        runBlocking {
            val n = db.extractLastSessionKeys(5)
            assertThat(n.size, equalTo(1))
            val klast = Base64.encodeToString(n[0].SecretKey, Base64.NO_WRAP)
            assertThat(klast, IsNot(equalTo(k4)))
            assertThat(klast, IsNot(equalTo(k3)))
            assertThat(klast, IsNot(equalTo(k1)))
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