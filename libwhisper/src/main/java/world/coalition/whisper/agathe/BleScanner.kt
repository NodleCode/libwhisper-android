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

import android.bluetooth.BluetoothDevice
import android.content.Context
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.Whisper
import world.coalition.whisper.WhisperCore
import world.coalition.whisper.agathe.android.BleScanRecord
import world.coalition.whisper.agathe.android.BluetoothScanner
import world.coalition.whisper.agathe.android.OnBleScanResultAvailable
import world.coalition.whisper.database.EventCode
import world.coalition.whisper.database.BleConnectEvent
import world.coalition.whisper.database.WhisperEvent
import kotlin.random.Random

/**
 * @author Lucien Loiseau on 27/03/20.
 */
class BleScanner(val core: WhisperCore) {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)
    private val bleAdvertising: BleAdvertising =
        BleAdvertising(core)
    private val bleGattServer: BleGattServer =
        BleGattServer(core)
    private val bleConnect: BleConnect =
        BleConnect(core)


    /**
     * Each phone advertises a 1-byte priority to decide which one of the two should connect during
     * a contact event. The phone with the highest priority will initiate the connection.
     * If it is equal, both may connect.
     *
     * To advertise the priority in the ble beacon, we use the Nodle manufacturer
     * specific data which is:
     * - 1 byte: payload type (must match the whisper type)
     * - 1 byte: priority
     */
    private val priority: ByteArray by lazy {
        val b = Random(System.currentTimeMillis()).nextBytes(2)
        b[0] = core.whisperConfig.nodlePayloadTypeWhisper
        log.info("set priority to:" + b[1])
        b
    }

    private fun currentDeviceHasPriority(scanRecord: BleScanRecord): Boolean {
        val peerPriority = scanRecord.manufacturerSpecificData
            ?.get(core.whisperConfig.nodleBluetoothManufacturerId)
            ?.takeIf { it.size >= 2 }
            ?.takeIf { it[0] == core.whisperConfig.nodlePayloadTypeWhisper }
            ?.get(1)
            ?: 0
        return priority[1] >= peerPriority
    }

    /**
     * This is where stuff happens.
     *
     * - step1: scan (filter only tracetogether or whisper) and add lower priority peer to the set
     * - step2: remove from the set peers that we recently connected to (but add a ping entry)
     * - step3: for each peer from the set, connect and read their secure ID synchronously
     * - step4: wait and goto step1
     */
    private var job: Job? = null
    fun start(context: Context, upstream: Channel<BleConnectEvent>) {
        if (job != null) return
        var lastPubKey = core.getPublicKey(context)
        job = CoroutineScope(Dispatchers.IO).launch {
            val scanner = BluetoothScanner.New(context)
            while (true) {
                bleGattServer.checkGATTServer(context)
                bleAdvertising.updateAdvertisingParameters(
                    context,
                    core.whisperConfig.nodleBluetoothManufacturerId,
                    priority
                )

                // step1 - SCANNING relevant service and build the peer connect set
                log.debug(">>> start scanning for ${core.whisperConfig.scannerScanDurationMillis / 1000} sec")
                val startScan = System.currentTimeMillis()
                val peerSet = HashMap<BluetoothDevice, Pair<Int, Boolean>>()
                val currentPubKey = core.getPublicKey(context)
                scanner.startScanCycle(
                    core.whisperConfig.scannerScanDurationMillis,
                    listOf(core.whisperConfig.whisperServiceUUID),
                    OnBleScanResultAvailable { d, r, p ->
                        if (peerSet[d] == null) {
                            // we add peer in connect if it has lower priority or if our temporary id was refreshed
                            if (currentDeviceHasPriority(p) ||  currentPubKey != lastPubKey) {
                                log.debug(">>> ${d.address} (${BleConnectEvent.Base64SHA256(d.address)}) : add peer to connect set")
                                peerSet[d] = Pair(r, true)
                            } else {
                                log.debug(">>> ${d.address} (${BleConnectEvent.Base64SHA256(d.address)}): ignore peer (has higher priority)")
                                peerSet[d] = Pair(r, false)
                            }
                        }
                    })
                val stopScan = System.currentTimeMillis()
                core.getDb(context).roomDb.whisperEventDao()
                    .insert(WhisperEvent(System.currentTimeMillis(), EventCode.SCAN_STOPPED.code, (stopScan-startScan).toInt(),peerSet.size,""))
                log.debug(">>> stop scanning")
                lastPubKey = currentPubKey

                // step 2 - FILTERING - we remove peer we recently connected to
                val now = System.currentTimeMillis()
                val connectSet = peerSet.filter {
                    // we get the last contact reading from this peripheral (if any)
                    val lastConnect = core.getDb(context).roomDb.bleConnectEventDao()
                        .getLastConnect(BleConnectEvent.Base64SHA256(it.key.address))

                    // we check if we need to remove connectable peer from connect set (throttling)
                    val rssi = it.value.first
                    val mustConnect = it.value.second
                    var mustThrottle = false
                    if (mustConnect && lastConnect != null) {
                        mustThrottle =
                            now < lastConnect.connectTimeMillis + core.whisperConfig.mustReconnectAfterMillis
                        if (mustThrottle) {
                            log.debug(">>> ${it.key.address} (${BleConnectEvent.Base64SHA256(it.key.address)}) ping too recent (${(now - lastConnect.connectTimeMillis) / 1000} sec), remove from connect set")
                        }
                    }

                    if(lastConnect != null && (!mustConnect || (mustConnect && mustThrottle))) {
                        log.debug(">>> ${it.key.address} (${BleConnectEvent.Base64SHA256(it.key.address)}) adding a ping")
                        core.getDb(context).addPing(lastConnect.petRowId, rssi, now, core.whisperConfig.pingMaxElapsedTimeMillis)
                    }

                    mustConnect && !mustThrottle
                }

                
                // step 3 - CONNECT - read peer secure id (if any)
                for (e in connectSet) {
                    if (!isActive) break
                    bleConnect.connectAndReadPeerId(
                        context,
                        e.key,
                        e.value.first,
                        upstream
                    )
                }
                log.debug(">>> no more device to connect to, sleep for ${core.whisperConfig.scannerWaitDurationMillis / 1000} sec")

                // step 4 - wait
                if (!isActive) break
                delay(core.whisperConfig.scannerWaitDurationMillis)
                if (!isActive) break
            }
        }
    }

    suspend fun stop() {
        job?.cancel()
        job?.join()
    }
}
