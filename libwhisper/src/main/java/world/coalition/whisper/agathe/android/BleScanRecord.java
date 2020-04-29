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

package world.coalition.whisper.agathe.android;

import android.bluetooth.le.ScanRecord;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.ArrayMap;
import android.util.SparseArray;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Represents a scan record from Bluetooth LE scan.
 *
 * copy from android.bluetooth.le.ScanRecord because constructor is private
 * and I need the parser to support ble < api level 23
 */
public final class BleScanRecord {
    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA_16_BIT = 0x16;
    private static final int DATA_TYPE_SERVICE_DATA_32_BIT = 0x20;
    private static final int DATA_TYPE_SERVICE_DATA_128_BIT = 0x21;
    private static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT = 0x14;
    private static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT = 0x1F;
    private static final int DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT = 0x15;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    private final int mAdvertiseFlags;
    private final List<ParcelUuid> mServiceUuids;
    private final List<ParcelUuid> mServiceSolicitationUuids;
    private final SparseArray<byte[]> mManufacturerSpecificData;
    private final Map<ParcelUuid, byte[]> mServiceData;
    private final int mTxPowerLevel;
    private final String mDeviceName;
    private final byte[] mBytes;

    public int getAdvertiseFlags() {
        return mAdvertiseFlags;
    }

    public @Nullable List<ParcelUuid> getServiceUuids() {
        return mServiceUuids;
    }

    public @Nullable List<ParcelUuid> getServiceSolicitationUuids() {
        return mServiceSolicitationUuids;
    }

    public @Nullable SparseArray<byte[]> getManufacturerSpecificData() {
        return mManufacturerSpecificData;
    }

    public @Nullable byte[] getManufacturerSpecificData(int manufacturerId) {
        if (mManufacturerSpecificData == null) {
            return null;
        }
        return mManufacturerSpecificData.get(manufacturerId);
    }

    public @Nullable Map<ParcelUuid, byte[]> getServiceData() {
        return mServiceData;
    }

    public @Nullable  byte[] getServiceData(ParcelUuid serviceDataUuid) {
        if (serviceDataUuid == null || mServiceData == null) {
            return null;
        }
        return mServiceData.get(serviceDataUuid);
    }

    public @Nullable int getTxPowerLevel() {
        return mTxPowerLevel;
    }

    public @Nullable String getDeviceName() {
        return mDeviceName;
    }

    public @Nullable byte[] getBytes() {
        return mBytes;
    }

    public BleScanRecord(ScanRecord record) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            mServiceSolicitationUuids = record.getServiceSolicitationUuids();
        } else {
            mServiceSolicitationUuids = new ArrayList<>(0);
        }

        mServiceUuids = record.getServiceUuids();
        mManufacturerSpecificData = record.getManufacturerSpecificData();
        mServiceData = record.getServiceData();
        mDeviceName = record.getDeviceName();
        mAdvertiseFlags = record.getAdvertiseFlags();
        mTxPowerLevel = record.getTxPowerLevel();
        mBytes = record.getBytes();
    }

    public BleScanRecord(List<ParcelUuid> serviceUuids,
                         List<ParcelUuid> serviceSolicitationUuids,
                         SparseArray<byte[]> manufacturerData,
                         Map<ParcelUuid, byte[]> serviceData,
                         int advertiseFlags, int txPowerLevel,
                         String localName, byte[] bytes) {
        mServiceSolicitationUuids = serviceSolicitationUuids;
        mServiceUuids = serviceUuids;
        mManufacturerSpecificData = manufacturerData;
        mServiceData = serviceData;
        mDeviceName = localName;
        mAdvertiseFlags = advertiseFlags;
        mTxPowerLevel = txPowerLevel;
        mBytes = bytes;
    }

    public static BleScanRecord parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return null;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        List<ParcelUuid> serviceSolicitationUuids = new ArrayList<ParcelUuid>();
        String localName = null;
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
        Map<ParcelUuid, byte[]> serviceData = new ArrayMap<ParcelUuid, byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength, BluetoothUuid.UUID_BYTES_16_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_16_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_16_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_32_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_32_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_SERVICE_SOLICITATION_UUIDS_128_BIT:
                        parseServiceSolicitationUuid(scanRecord, currentPos, dataLength,
                                BluetoothUuid.UUID_BYTES_128_BIT, serviceSolicitationUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA_16_BIT:
                    case DATA_TYPE_SERVICE_DATA_32_BIT:
                    case DATA_TYPE_SERVICE_DATA_128_BIT:
                        int serviceUuidLength = BluetoothUuid.UUID_BYTES_16_BIT;
                        if (fieldType == DATA_TYPE_SERVICE_DATA_32_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_32_BIT;
                        } else if (fieldType == DATA_TYPE_SERVICE_DATA_128_BIT) {
                            serviceUuidLength = BluetoothUuid.UUID_BYTES_128_BIT;
                        }

                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = BluetoothUuid.parseUuidFrom(
                                serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8)
                                + (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }

            return new BleScanRecord(serviceUuids, serviceSolicitationUuids, manufacturerData,
                    serviceData, advertiseFlag, txPowerLevel, localName, scanRecord);
        } catch (Exception e) {
            return new BleScanRecord(null, null, null, null, -1, Integer.MIN_VALUE, null, scanRecord);
        }
    }

    private static int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                        int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    private static int parseServiceSolicitationUuid(byte[] scanRecord, int currentPos,
                                                    int dataLength, int uuidLength,
                                                    List<ParcelUuid> serviceSolicitationUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos, uuidLength);
            serviceSolicitationUuids.add(BluetoothUuid.parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    private static byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }
}
