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

package world.coalition.whisper.agathe.android

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.PermissionChecker

/**
 * @author Lucien Loiseau on 27/03/20.
 */
class BluetoothUtil {
    companion object {
        fun checkBluetoothPermission(context: Context): Boolean {
            return (PermissionChecker.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PermissionChecker.PERMISSION_GRANTED &&
                PermissionChecker.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PermissionChecker.PERMISSION_GRANTED)
        }

        fun checkBLEPermission(context: Context): Boolean {
            return (PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PermissionChecker.PERMISSION_GRANTED &&
                    PermissionChecker.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PermissionChecker.PERMISSION_GRANTED)
        }

        fun checkBluetoothOn(context: Context): Boolean {
            val mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
            return mBluetoothAdapter?.isEnabled ?: false
        }

        fun checkBluetoothLE(context: Context): Boolean {
            return context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        }
    }
}