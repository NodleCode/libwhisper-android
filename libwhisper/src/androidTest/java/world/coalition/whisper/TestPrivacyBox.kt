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

import android.location.Location
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.matcher.ViewMatchers.assertThat
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.Matchers.*
import org.junit.FixMethodOrder
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters
import world.coalition.whisper.database.BoundingBox
import world.coalition.whisper.database.LocationUpdate
import world.coalition.whisper.database.WhisperDatabase
import world.coalition.whisper.geo.GeoUtil

/**
 * @author Lucien Loiseau on 08/04/20.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
class TestPrivacyBox {
    private var db = WhisperDatabase.persistent(ApplicationProvider.getApplicationContext())

    @Test
    fun stage0_init() {
        db.roomDb.clearAllTables()
        Thread.sleep(500)
    }

    @Test
    fun stage1_init() {
        db.roomDb.locationUpdateDao().insert(LocationUpdate(0, 1.0,100.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(1, 2.0,90.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(2, 3.0,80.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(3, 4.0,70.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(4, 5.0,60.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(5, 6.0,60.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(6, 7.0,40.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(7, 8.0,30.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(8, 9.0,20.0,1.0))
        db.roomDb.locationUpdateDao().insert(LocationUpdate(9, 10.0,10.0,1.0))

        var bb = db.roomDb.locationUpdateDao().boundingBox(0)
        assertThat(bb, notNullValue())
        assertThat(bb?.minLatitude, equalTo(1.0))
        assertThat(bb?.minLongitude, equalTo(10.0))
        assertThat(bb?.maxLatitude, equalTo(10.0))
        assertThat(bb?.maxLongitude, equalTo(100.0))


        bb = db.roomDb.locationUpdateDao().boundingBox(5)
        assertThat(bb, notNullValue())
        assertThat(bb?.minLatitude, equalTo(6.0))
        assertThat(bb?.minLongitude, equalTo(10.0))
        assertThat(bb?.maxLatitude, equalTo(10.0))
        assertThat(bb?.maxLongitude, equalTo(60.0))

        Thread.sleep(500)
    }

    @Test
    fun stage2_fuzzy() {
        val bbox = BoundingBox(1.311338,103.886460, 1.313741,   103.900622)
        val loc1 = Location("1")
        loc1.latitude = bbox.minLatitude
        loc1.longitude = bbox.minLongitude
        val loc2 = Location("1")
        loc2.latitude = bbox.minLatitude
        loc2.longitude = bbox.maxLongitude
        val dist = loc1.distanceTo(loc2)
        assertThat(dist, lessThanOrEqualTo(10000.0.toFloat()))

        val newbbox = GeoUtil.fuzzyBoundingBox(bbox, 10000)
        val loc3 = Location("1")
        loc3.latitude = newbbox.minLatitude
        loc3.longitude = newbbox.minLongitude
        val loc4 = Location("1")
        loc4.latitude = newbbox.minLatitude
        loc4.longitude = newbbox.maxLongitude
        val dist2 = loc3.distanceTo(loc4)
        assertThat(dist2, greaterThanOrEqualTo(10000.0.toFloat()))
    }

}