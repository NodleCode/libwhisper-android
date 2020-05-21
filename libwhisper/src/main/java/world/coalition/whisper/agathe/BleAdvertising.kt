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

package world.coalition.whisper.agathe

import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.Whisper
import world.coalition.whisper.WhisperCore
import world.coalition.whisper.agathe.android.BluetoothUtil

/**
 * @author Lucien Loiseau on 27/03/20.
 */
class BleAdvertising(val core: WhisperCore) {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)

    fun updateAdvertisingParameters(context: Context, manufacturerId: Int, manufacturerData: ByteArray) {
        if (!BluetoothUtil.checkBluetoothOn()) return

        val settings = AdvertiseSettings.Builder()
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addServiceUuid(ParcelUuid(core.whisperConfig.whisperServiceUUID))
            .addManufacturerData(manufacturerId, manufacturerData)
            .build()

        (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)
            ?.adapter
            ?.bluetoothLeAdvertiser
            ?.startAdvertising(settings, data, mAdvertiseCallback)
    }

    private val mAdvertiseCallback by lazy {
        object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                super.onStartSuccess(settingsInEffect)
                log.info("[+] advertising started")
            }

            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                log.info("[!] advertising failed to start: $errorCode")
            }
        }
    }
}