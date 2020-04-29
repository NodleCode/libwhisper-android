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

import android.bluetooth.*
import android.bluetooth.BluetoothGatt.GATT_SUCCESS
import android.content.Context
import android.util.Base64
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.Whisper
import world.coalition.whisper.WhisperCore
import world.coalition.whisper.database.PeerContactEvent
import java.security.SecureRandom
import java.util.*

/**
 * @author Lucien Loiseau on 30/03/20.
 */
class BleConnect(val core: WhisperCore) {

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)
    private val random = SecureRandom()

    /**
     * connectAndReadPeerId, this is blocking!
     * @param context the Android application Context
     * @param device to connect to
     * @param upstream channel to send read id
     */
    suspend fun connectAndReadPeerId(
        context: Context,
        device: BluetoothDevice,
        rssi: Int,
        upstream: Channel<PeerContactEvent>
    ) {
        /**
         * They are 7 asynchronous STEPS that must be performed syncronously.
         * If any step fail, we immediately disconnect (except for increase MTU)
         *
         * 1 ..... connect
         * 2 ..... try to increase MTU size (may be refused)
         * 3 ..... discover services
         * 4 ..... write characteristic (challenge1)
         * 5 ..... read whisper characteristic (peer tid + challenge1 + hmac, challenge2)
         * 6 ..... write characteristic (send own tid + challenge2 + hmac)
         * 7 ..... disconnect
         * 8 ..... release the mutex
         */
        val mutex = Mutex(true)

        /**
         * STEP 1 - we connect to the device
         */
        log.debug("device: ${device.address} > connecting..")
        device.connectGatt(context, false, object : BluetoothGattCallback() {
            var step = 1

            private var mtu = 20 // step 2
            private val challengeToSend = ByteArray(6)  // step 4
            private var challengeFromPeer: ByteArray? = null // step 5
            private val todo = LinkedList<BluetoothGattCharacteristic>() // step 5X

            private fun step2(gatt: BluetoothGatt) {
                step = 2
                log.debug("device: ${device.address} > request MTU 20 ...")
                if (!gatt.requestMtu(80)) {
                    log.debug("device: ${device.address} < request MTU failed (ignored)")
                    step3(gatt)
                }
            }

            private fun step3(gatt: BluetoothGatt) {
                step = 3
                log.debug("device: ${device.address} > discover services ...")
                if (!gatt.discoverServices()) {
                    log.debug("device: ${device.address} < discovery failed")
                    step7(gatt)
                }
            }

            private fun step4(gatt: BluetoothGatt) {
                step = 4
                val characteristic = gatt.services
                    ?.last { it.uuid == core.whisperConfig.whisperServiceUUID}
                    ?.getCharacteristic(core.whisperConfig.whisperCharacteristicUUID)

                if (characteristic != null) {
                    // create challenge
                    random.nextBytes(challengeToSend)

                    // sending it over
                    characteristic.value = byteArrayOf(0x00,0x06) + challengeToSend
                    log.debug("device: ${device.address} > writing challenge...")
                    if (gatt.writeCharacteristic(characteristic)) {
                        return
                    }
                }

                log.debug("device: ${device.address} < writing challenge failed!")
                step7(gatt)
            }

            private fun step5(gatt: BluetoothGatt) {
                step = 5
                gatt.services
                    .filter {
                        it.uuid == core.whisperConfig.whisperServiceUUID
                    }.map {
                        todo.add(it.getCharacteristic(core.whisperConfig.whisperCharacteristicUUID))
                    }
                step5X(gatt)
            }

            private fun step5X(gatt: BluetoothGatt) {
                step = 5
                if (!todo.isEmpty()) {
                    log.debug("device: ${device.address} > read characteristic ${todo.peek()?.uuid} ...")
                    if (!gatt.readCharacteristic(todo.pop())) {
                        log.debug("device: ${device.address} < read failed!")
                        step7(gatt)
                    }
                } else {
                    step6(gatt)
                }
            }

            var timeout: Job? = null // FIXME we should not have to rely on this
            private fun step6(gatt: BluetoothGatt) {
                step = 6
                val characteristic = gatt.services
                    ?.last { it.uuid == core.whisperConfig.whisperServiceUUID }
                    ?.getCharacteristic(core.whisperConfig.whisperCharacteristicUUID)

                if (characteristic != null && challengeFromPeer != null) {
                    // create tid payload
                    val payload = ProtoBuf.dump(
                        TIDPayload.serializer(),
                        TIDPayload(
                            1,
                            core.whisperConfig.organizationCode,
                            core.getSecureId(challengeFromPeer!!)
                        )
                    )

                    // sending it over
                    characteristic.value = byteArrayOf(0x02,payload.size.toByte())+payload
                    log.debug("device: ${device.address} > writing challenge...")
                    if (gatt.writeCharacteristic(characteristic)) {
                        // FIXME if MTU is too small and multiple GATT packet must be sent
                        // sometimes the onCharacteristicWrite callback is not called even though
                        // all gatt packets are received on the peer.
                        // the following timer is to avoid waiting the hardcoded 30 sec for
                        // the timeout
                        timeout = GlobalScope.launch {
                            delay(5000)
                            if(isActive) {
                                log.debug("device: ${device.address} > //fixme// timeout fired!")
                                timeout = null
                                step7(gatt)
                            }
                        }
                        return
                    }
                }

                log.debug("device: ${device.address} > writing failed!")
                step7(gatt)
            }

            private fun step7(gatt: BluetoothGatt) {
                step = 7
                log.debug("device: ${device.address} > disconnecting ...")
                gatt.disconnect()
            }

            private fun step8() {
                step = 8
                log.debug("device ${device.address} > unlocking mutex")
                mutex.unlock()
            }


            // step 1 and 6 callback
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                log.debug("device: ${device.address} < connection changed $newState")
                if (status == GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                    step2(gatt)
                } else {
                    step8()
                }
            }

            // step 2 callback
            override fun onMtuChanged(gatt: BluetoothGatt?, newMtu: Int, status: Int) {
                log.debug("device: ${device.address} < mtu changed ($newMtu)")
                mtu = newMtu - 3 // todo: why do I seem to only be able to send MTU-3 bytes?!
                if (gatt == null) {
                    return step8()
                }
                step3(gatt)
            }

            // step3 callback
            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                log.debug("device: ${device.address} < service discovered ($status)")
                if (status == GATT_SUCCESS) {
                    return step4(gatt)
                }
                return step7(gatt)
            }

            // step 5 callback
            override fun onCharacteristicRead(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                log.debug("device: ${device.address} < characteristic read ($status)")
                if (gatt == null) return step8()
                if (characteristic == null || status != GATT_SUCCESS) {
                    return step7(gatt)
                }

                processCoalitionPayload(characteristic)
                step5X(gatt)
            }

            // callback step 4 (write challenge) and 6 (write secure tid)
            override fun onCharacteristicWrite(
                gatt: BluetoothGatt?,
                characteristic: BluetoothGattCharacteristic?,
                status: Int
            ) {
                timeout?.cancel()
                when(step) {
                    4 -> {
                        log.debug("device: ${device.address} < characteristic write challenge ($status)")
                        if (gatt == null) return step8()
                        step5(gatt)
                    }
                    6 -> {
                        log.debug("device: ${device.address} < characteristic write tid ($status)")
                        if (gatt == null) return step8()
                        step7(gatt)
                    }
                    else -> {
                        // fatal but it should never happen
                        if (gatt == null) return step8()
                        step7(gatt)
                    }
                }
            }

            fun processCoalitionPayload(characteristic: BluetoothGattCharacteristic) {
                if (characteristic.uuid != core.whisperConfig.whisperCharacteristicUUID) return
                if (characteristic.value == null) return

                log.debug("device: ${device.address} < decoding wisper payload.. (size=${characteristic.value.size})")
                try {
                    if (characteristic.value.size < 2) throw Exception("header: missing")
                    val cmd = characteristic.value[0]
                    val payloadSize = characteristic.value[1]

                    if (cmd != 0x01.toByte()) throw Exception("header: unexpected type")
                    if (payloadSize+2 > characteristic.value.size) throw Exception("header: wrong size")

                    val payload = ProtoBuf.load(
                        TIDWithChallengePayload.serializer(),
                        characteristic.value.sliceArray(2..payloadSize+1))

                    if(payload.challenge1.size != challengeToSend.size
                        || !payload.challenge1.contentEquals(challengeToSend)) throw Exception("secureid: challenge doesn't match")

                    challengeFromPeer = payload.challenge2
                    log.debug("device: ${device.address} < whisper secureid ${Base64.encodeToString(payload.temporaryId,Base64.NO_WRAP)}")

                    // channel as infinite buffer so it can't block but still need to wrap
                    // otherwise this fun would require "suspend"
                    CoroutineScope(Dispatchers.IO).launch {
                        upstream.send(
                            PeerContactEvent.fromTIDWithChallengePayload(
                                payload,
                                device.address,
                                rssi,
                                System.currentTimeMillis()
                            )
                        )
                    }
                } catch (e: Exception) {
                    log.debug("device: ${device.address} < parser failed! $e")
                }
                // badly formatted payload, do nothing
            }
        })
        mutex.lock()
    }
}