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
import android.bluetooth.BluetoothDevice
import android.content.Context
import java.util.*

/**
 * @author Lucien Loiseau on 30/03/20.
 */
class BluetoothLELegacy(ctx: Context) :
    BluetoothScanner(ctx) {

    private lateinit var leCb: BluetoothAdapter.LeScanCallback

    override fun onStartScan(UUIDs: List<UUID>?): Boolean {
        leCb =
            BluetoothAdapter.LeScanCallback { device: BluetoothDevice?, rssi: Int, scanRecord: ByteArray? ->
                cb.onBleScanResultAvailable(
                    device,
                    rssi,
                    BleScanRecord.parseFromBytes(scanRecord)
                )
            }

        val filter: Array<UUID>? = UUIDs?.toTypedArray()

        return btAdapter?.startLeScan(filter, leCb) ?: false
    }

    override fun onStopScan() {
        btAdapter?.stopLeScan(leCb)
    }

    private fun isValidUUID(UUID: String): Boolean {
        return try {
            java.util.UUID.fromString(UUID)
            true
        } catch (e: Exception) {
            false
        }
    }
}