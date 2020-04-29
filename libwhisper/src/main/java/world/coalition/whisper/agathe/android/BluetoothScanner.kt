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

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.delay
import java.util.*

/**
 * @author Lucien Loiseau on 30/03/20.
 */
abstract class BluetoothScanner(val context: Context)  {

    companion object {
        fun New(context: Context): BluetoothScanner {
            return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                BluetoothLELegacy(
                    context
                )
            } else {
                BluetoothLE(context)
            }
        }
    }

    private val btManager: BluetoothManager? = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    protected val btAdapter: BluetoothAdapter? = btManager?.adapter
    lateinit var cb: OnBleScanResultAvailable

    suspend fun startScanCycle(time: Long, UUIDs: List<UUID>?, cb: OnBleScanResultAvailable): Boolean {
        this@BluetoothScanner.cb = cb
        if (!onStartScan(UUIDs)) return false
        delay(time)
        onStopScan()
        return true
    }

    protected abstract fun onStartScan(UUIDs: List<UUID>?): Boolean
    protected abstract fun onStopScan()
}