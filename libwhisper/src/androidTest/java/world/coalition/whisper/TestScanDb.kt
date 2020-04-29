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

import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers
import org.junit.Test
import org.junit.runner.RunWith
import world.coalition.whisper.database.EventCode
import world.coalition.whisper.database.WhisperEvent
import world.coalition.whisper.database.WhisperDatabase

/**
 * @author Lucien Loiseau on 04/04/20.
 */
@RunWith(AndroidJUnit4::class)
class TestScanDb {

    private var db = WhisperDatabase.persistent(
        ApplicationProvider.getApplicationContext())

    @Test
    fun databaseTest() {
        stage0_init()
        stage6_closeDb()
    }

    fun stage0_init() {
        db.roomDb.clearAllTables()
        Thread.sleep(500)
    }

    fun stage1_addScanEvent() {
        var dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(0))

        db.roomDb.whisperEventDao().insert(WhisperEvent(10, EventCode.SCAN_STARTED.code,0,0,""))
        dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(1))

        db.roomDb.whisperEventDao().insert(WhisperEvent(18,  EventCode.SCAN_STOPPED.code,5,0, ""))
        dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(2))
    }

    fun stage2_deleteScanEvent() {
        var dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(0))

        db.roomDb.whisperEventDao().pruneOldData(15)
        dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(1))

        db.roomDb.whisperEventDao().insert(WhisperEvent(20, EventCode.SCAN_STOPPED.code,0,0,""))
        dumps = db.roomDb.whisperEventDao().getAll()
        ViewMatchers.assertThat(dumps.size, CoreMatchers.equalTo(0))
    }

    fun stage5_testTearDown() {
        db.roomDb.clearAllTables()
    }

    fun stage6_closeDb() {
        db.close()
    }


}