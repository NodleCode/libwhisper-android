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
import android.content.Context
import android.util.Base64
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.protobuf.ProtoBuf
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import world.coalition.whisper.Whisper
import world.coalition.whisper.WhisperCore
import world.coalition.whisper.agathe.android.BluetoothUtil
import world.coalition.whisper.database.PeerContactEvent
import java.security.SecureRandom

/**
 * @author Lucien Loiseau on 27/03/20.
 */
class BleGattServer(val core: WhisperCore) {
    
    class DeviceState {
        var step: Int = 1
        var readRequestPeerChallenge: ByteArray? = null
        var readRequestResponseChallenge: ByteArray = ByteArray(6)
        var readRequestResponseBuffer: ByteArray? = null
        var writeRequestBuffer: ByteArray? = null
    }

    private val log: Logger = LoggerFactory.getLogger(Whisper::class.java)
    private var mGattServer: BluetoothGattServer? = null
    private var mGattServerCallback: BluetoothGattServerCallback? = null
    private val random = SecureRandom()


    /**
     * For each node that connect, we follow the following steps:
     *
     * step0... waiting for connection
     * step1... waiting for receiving a write request (challenge)
     * step2... waiting for receiving a read request
     *          -> replying with secure ID + new challenge
     * step3... waiting for receiving a write request (peer's secure ID)
     */
    private fun getGattServerCallback(): BluetoothGattServerCallback {
        return mGattServerCallback ?: let {
            mGattServerCallback = object : BluetoothGattServerCallback() {

                // FIXME add mutex everywhere
                val state = HashMap<BluetoothDevice, DeviceState>()
                private fun getState(device: BluetoothDevice): DeviceState {
                    return state[device] ?: let {
                        log.warn("device ${device.address} hasn't been properly initialized")
                        state[device] = DeviceState()
                        state[device]!!
                    }
                }

                override fun onConnectionStateChange(
                    device: BluetoothDevice?,
                    status: Int,
                    newState: Int
                ) {
                    super.onConnectionStateChange(device, status, newState)
                    if (newState == BluetoothProfile.STATE_CONNECTED && device != null) {
                        log.warn("device ${device.address} < connected")
                        state[device] = DeviceState()
                    }
                    if (newState == BluetoothProfile.STATE_DISCONNECTED && device != null) {
                        log.warn("device ${device.address} < disconnected")
                        state.remove(device)
                    }
                }

                override fun onCharacteristicReadRequest(
                    device: BluetoothDevice,
                    requestId: Int,
                    offset: Int,
                    characteristic: BluetoothGattCharacteristic
                ) {
                    super.onCharacteristicReadRequest(device, requestId, offset, characteristic)
                    if (getState(device).step != 2) {
                        log.warn("device ${device.address} < read request unexpected!")
                        return fail(device, requestId)
                    }

                    if (getState(device).readRequestPeerChallenge == null) {
                        log.warn("device ${device.address} < read request but no challenge!")
                        return fail(device, requestId)
                    } else {
                        log.warn("device ${device.address} < read request, replying with secure id")
                        getState(device).step = 3
                        return success(device, requestId, offset, response(device).sliceArray(offset until response(device).size))
                    }
                }

                private fun response(device: BluetoothDevice): ByteArray {
                    return getState(device).readRequestResponseBuffer ?: let {
                        random.nextBytes(getState(device).readRequestResponseChallenge)
                        val payload = ProtoBuf.dump(
                            TIDWithChallengePayload.serializer(),
                            TIDWithChallengePayload(
                                1,
                                core.whisperConfig.organizationCode,
                                core.getSecureId(state[device]!!.readRequestPeerChallenge!!),
                                getState(device).readRequestResponseChallenge
                            )
                        )
                        getState(device).readRequestResponseBuffer =
                            byteArrayOf(0x01, payload.size.toByte()) + payload
                        getState(device).readRequestResponseBuffer!!
                    }
                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice?,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic?,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    if (device == null) return
                    if (value == null) return fail(device, requestId)
                    if (value.size < 2) return fail(device, requestId)

                    log.info("device ${device.address} < write request - frame: size=${value.size} offset=${offset}")

                    getState(device).writeRequestBuffer =
                        getState(device).writeRequestBuffer?.plus(value) ?: value
                    val cmd = getState(device).writeRequestBuffer!![0].toInt()
                    val expectedPayloadSize = getState(device).writeRequestBuffer!![1].toInt()

                    if (getState(device).writeRequestBuffer!!.size < expectedPayloadSize + 2) {
                        // we need more data
                        return success(device, requestId, offset, value)
                    }

                    log.info("device ${device.address} < write request - recv full payload")
                    when (cmd) {
                        0x00 -> {
                            if (getState(device).step != 1) {
                                log.info("device ${device.address} < write request - unexpected")
                                return fail(device, requestId)
                            }

                            // alice ---send challenge---> bob
                            getState(device).readRequestPeerChallenge =
                                getState(device).writeRequestBuffer?.sliceArray(2..1 + expectedPayloadSize)
                            log.info(
                                "device ${device.address} < write request - recv challenge: " + Base64.encodeToString(
                                    getState(device).readRequestPeerChallenge,
                                    Base64.NO_WRAP
                                )
                            )
                            getState(device).step = 2
                            getState(device).writeRequestBuffer = null
                            return success(device, requestId, offset, value)
                        }
                        0x02 -> {
                            // alice ---send TID---> bob
                            try {
                                if (getState(device).step != 3) {
                                    log.info("device ${device.address} < write request - unexpected")
                                    return fail(device, requestId)
                                }

                                val tidPayload = ProtoBuf.load(
                                    TIDPayload.serializer(),
                                    getState(device).writeRequestBuffer!!.sliceArray(2..1 + expectedPayloadSize)
                                )
                                // check that the received challenge matches the one we sent
                                if (!tidPayload.challenge.contentEquals(getState(device).readRequestResponseChallenge)) {
                                    throw Exception("recv challenge does not match the one we sent")
                                }

                                log.debug("device: ${device.address} < write request - whisper secureid ${Base64.encodeToString(tidPayload.temporaryId, Base64.NO_WRAP)}")
                                runBlocking {
                                    core.channel.send(
                                        PeerContactEvent.fromTIDPayload(
                                            tidPayload,
                                            device.address,
                                            0,
                                            System.currentTimeMillis()
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                log.debug("device: ${device.address} < write request - parser failed! $e")
                                return fail(device, requestId)
                            }
                            // preemptive cleaning
                            state.remove(device)
                            return success(device, requestId, offset, value)
                        }
                        else -> {
                            log.info("device ${device.address} < frame type unknown")
                            // preemptive cleaning
                            state.remove(device)
                            return fail(device, requestId)
                        }
                    }
                }


                fun fail(device: BluetoothDevice, requestId: Int) {
                    mGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null
                    )
                }

                fun success(
                    device: BluetoothDevice,
                    requestId: Int,
                    offset: Int,
                    value: ByteArray
                ) {
                    mGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        offset,
                        value
                    )
                }
            }

            mGattServerCallback!!
        }
    }

    fun checkGATTServer(context: Context) {
        if (!BluetoothUtil.checkBluetoothOn(context)) return
        if (!BluetoothUtil.checkBluetoothLE(context)) return

        if (mGattServer == null) {
            val whisperGattService = BluetoothGattService(
                core.whisperConfig.whisperServiceUUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )

            val whisperGattCharacteristic = BluetoothGattCharacteristic(
                core.whisperConfig.whisperCharacteristicUUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT,
                BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
            )

            whisperGattService.addCharacteristic(whisperGattCharacteristic)

            mGattServer = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager?)
                ?.openGattServer(context, getGattServerCallback())
            mGattServer?.addService(whisperGattService)
        }
    }
}