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

package world.coalition.whisper

import java.util.*
import kotlin.collections.HashMap

/**
 * @author Lucien Loiseau on 27/03/20.
 */
class WhisperConfig {
    /* organization code */
    var organizationCode: Int = 0x01

    /* location tracker parameters */
    var enablePrivacyBox: Boolean = false
    var locationUpdateDistance: Float = 0.0f
    var locationUpdateIntervalMillis: Long = 120000L // 2 minutes
    var minimumBoundingBoxSize: Int = 100000 // 100 km

    /* Secure Id Parameters */
    var sessionKeyValiditySec: Int = 3600 * 24 * 7 // 1 week
    var temporaryIdValiditySec: Int = 3600 // 1 hour
    var incubationPeriod: Int = 3600 * 24 * 7 * 3  // 3 weeks

    /* BLE advertising */
    var nodleBluetoothManufacturerId: Int = 0x076c
    var nodlePayloadTypeWhisper: Byte = 0x03

    /* BLE Scanner logic */
    var scannerWaitDurationMillis: Long = 60 * 1000 // 1 minute
    var scannerScanDurationMillis: Long = 8 * 1000  // 8 seconds
    var mustReconnectAfterMillis: Long = 30 * 60 * 1000 // 30 minutes
    var pingMaxElapsedTimeMillis: Long = 5 * 60 * 1000 // 5 minutes

    /* GATT services and characteristics */
    var whisperServiceUUID: UUID = UUID.fromString("1e91022a-4c2a-434d-be23-d39eb6cd4952")
    var whisperCharacteristicUUID: UUID = UUID.fromString("4d5c8851-6210-425f-8ab9-df679779a3b4")
}