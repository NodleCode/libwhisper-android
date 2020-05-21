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

import android.Manifest
import android.content.Context
import android.location.Criteria
import android.location.Criteria.ACCURACY_LOW
import android.location.Criteria.POWER_LOW
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.HandlerThread
import androidx.core.content.PermissionChecker
import ch.hsr.geohash.GeoHash
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.BuildConfig
import world.coalition.whisper.Whisper
import world.coalition.whisper.WhisperCore

/**
 * TODO this is not the most battery friendly approach better use play service.
 * @author Lucien Loiseau on 08/04/20.
 */
class GpsListener(val core: WhisperCore) : LocationListener {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)
    private var mLocationManager: LocationManager? = null
    private var mHandlerThread: HandlerThread = HandlerThread("gps-listener")
    private val mCriteria: Criteria = createLocationCriteria()
    private var listener: (Location) -> Unit = {}

    private fun checkPermissions(context: Context): Boolean {
        return (PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED
                && PermissionChecker.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PermissionChecker.PERMISSION_GRANTED)
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    fun start(context: Context, listener: (Location) -> Unit) {
        if (!core.whisperConfig.enablePrivacyBox) return
        if (!checkPermissions(context)) return
        if (!isLocationEnabled(context)) return
        if (mHandlerThread.isAlive) return
        mHandlerThread.start()

        this.listener = listener
        mLocationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val bestProvider = mLocationManager?.getBestProvider(mCriteria, true) ?: return
        try {
            mLocationManager?.requestLocationUpdates(
                bestProvider,
                core.whisperConfig.locationUpdateIntervalMillis,
                core.whisperConfig.locationUpdateDistance,
                this,
                mHandlerThread.looper
            )
        } catch (e: SecurityException) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace()
            }
            // permission failure
        }
    }

    fun stop() {
        mLocationManager?.removeUpdates(this)
        listener = {}
        mHandlerThread.quitSafely()
    }

    private fun createLocationCriteria(): Criteria {
        val criteria = Criteria()
        criteria.powerRequirement = POWER_LOW
        criteria.isCostAllowed = false
        criteria.accuracy = ACCURACY_LOW
        return criteria
    }


    // LocationListener
    override fun onLocationChanged(location: Location?) {
        if (location != null) {
            log.debug(">>> location update: (${location.latitude}, ${location.longitude}, ${location.altitude})")
            listener(location)
        }
    }

    // LocationListener
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    // LocationListener
    override fun onProviderEnabled(provider: String?) {}

    // LocationListener
    override fun onProviderDisabled(provider: String?) {}

}