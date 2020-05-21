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

import android.annotation.TargetApi
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

/**
 * @author Lucien Loiseau on 30/03/20.
 */
@TargetApi(23)
class BluetoothLE(ctx: Context) : BluetoothScanner(ctx) {

    private val log: Logger = LoggerFactory.getLogger(BluetoothLE::class.java)
    private var mScanner: BluetoothLeScanner? = null
    private var mSettings: ScanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
        .setNumOfMatches(1)
        .setReportDelay(0)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val mBleScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            cb.onBleScanResultAvailable(
                result.device,
                result.rssi,
                BleScanRecord(result.scanRecord)
            )
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            super.onBatchScanResults(results)
            for (element in results) {
                cb.onBleScanResultAvailable(
                    element.device,
                    element.rssi,
                    BleScanRecord(
                        element.scanRecord
                    )
                )
            }
        }
    }

    override fun onStartScan(UUIDs: List<UUID>?): Boolean {
        if (!checkBLE()) return false
        return try {
            val filter: List<ScanFilter>? = UUIDs
                ?.filter { isValidUUID(it) }
                ?.map { u -> ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid(u))
                    .build()
                }

            mScanner?.startScan(filter, mSettings, mBleScanCallback)
            true
        } catch (e: IllegalStateException) {
            false
        }
    }

    override fun onStopScan() {
        try {
            mScanner?.stopScan(mBleScanCallback)
        } catch (e: Exception) {
        }
    }

    private fun checkBLE(): Boolean {
        if (!BluetoothUtil.checkBluetoothPermission(context)) return false
        if (!BluetoothUtil.checkBLEPermission(context)) return false
        if (!BluetoothUtil.checkBluetoothOn()) return false
        if (mScanner != null) return true

        return try {
            mScanner = btAdapter?.bluetoothLeScanner!!
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidUUID(Uuid: UUID): Boolean {
        return try {
            ParcelUuid(Uuid)
            true
        } catch (e: Exception) {
            false
        }
    }
}