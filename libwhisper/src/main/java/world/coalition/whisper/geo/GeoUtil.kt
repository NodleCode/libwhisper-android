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

package world.coalition.whisper.geo

import android.location.Location
import world.coalition.whisper.database.BoundingBox
import java.lang.Math.toDegrees
import java.lang.Math.toRadians
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random


/**
 * @author Lucien Loiseau on 10/04/20.
 */
object GeoUtil {

    private const val EARTH_RADIUS = 6371009.0

    fun moveByOffset(from: Location, heading: Double, distance: Double) {
        val edistance = distance / EARTH_RADIUS
        val eheading = toRadians(heading)
        val fromLat: Double = toRadians(from.latitude)
        val fromLng: Double = toRadians(from.longitude)
        val cosDistance: Double = cos(edistance)
        val sinDistance: Double = sin(edistance)
        val sinFromLat: Double = sin(fromLat)
        val cosFromLat: Double = cos(fromLat)
        val sinLat: Double = cosDistance * sinFromLat + sinDistance * cosFromLat * cos(eheading)
        val dLng: Double = atan2(
            sinDistance * cosFromLat * sin(eheading),
            cosDistance - sinFromLat * sinLat
        )

        from.latitude = toDegrees(asin(sinLat))
        from.longitude = toDegrees(fromLng + dLng)
    }

    fun fuzzyBoundingBox(from: BoundingBox, minimumDistance: Int): BoundingBox {
        val southwest = Location("southwest")
        southwest.latitude = from.minLatitude
        southwest.longitude = from.minLongitude

        val northeast = Location("northeast")
        northeast.latitude = from.maxLatitude
        northeast.longitude = from.maxLongitude

        val southeast = Location("southeast")
        southeast.latitude = from.minLatitude
        southeast.longitude = from.maxLongitude

        val northsouthdistance: Int = northeast.distanceTo(southeast).toInt()
        val eastwestdistance: Int = southwest.distanceTo(southeast).toInt()

        if (northsouthdistance < minimumDistance) {
            // need to increase min max latitude
            val offset = minimumDistance - northsouthdistance
            val randomPadding = Random.nextInt(offset)
            val southOffset = offset/2 + randomPadding
            val northOffset = offset/2 + (offset-randomPadding)
            moveByOffset(northeast, 0.0, northOffset.toDouble())
            moveByOffset(southwest, 180.0, southOffset.toDouble())
        }

        if (eastwestdistance < minimumDistance) {
            // need to increase min max longitude
            val offset = minimumDistance - eastwestdistance
            val randomPadding = Random.nextInt(offset)
            val westOffset = offset/2 + randomPadding
            val eastOffset = offset/2 + (offset-randomPadding)
            moveByOffset(southwest, 270.0, westOffset.toDouble())
            moveByOffset(northeast, 90.0, eastOffset.toDouble())
        }

        return BoundingBox(
            minOf(southwest.latitude, northeast.latitude),
            minOf(southwest.longitude,northeast.longitude),
            maxOf(southwest.latitude, northeast.latitude),
            maxOf(southwest.longitude,northeast.longitude)
        )
    }

}