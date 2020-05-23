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

package world.coalition.whisper.database

import android.util.Base64
import androidx.annotation.NonNull
import androidx.room.*
import org.bouncycastle.jcajce.provider.digest.SHA256

/**
 * @author Lucien Loiseau on 09/04/20.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = PrivateEncounterToken::class,
            parentColumns = ["row_id"],
            childColumns = ["pet_rowid"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
class BleConnectEvent(
    @ColumnInfo(name = "initiator") val initiator: Boolean,
    @ColumnInfo(name = "peripheral_hash", index = true) var peripheralHash: String,
    @ColumnInfo(name = "connect_time_ms") val connectTimeMillis: Long,
    @ColumnInfo(name = "organization") val organization: Int,
    @ColumnInfo(name = "adv_v") val version: Int,
    @ColumnInfo(name = "adv_pubkey") var advPeerPubKey: String,
    @ColumnInfo(name = "pet_rowid", index = true) var petRowId: Long,
    @ColumnInfo(name = "rssi") val rssi: Int
) {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id", index = true)
    var id: Long = 0

    constructor(
        initiator: Boolean,
        peripheral: String,
        receivedTime: Long,
        organization: Int,
        version: Int,
        peerPubKey: ByteArray,
        rssi: Int
    ) : this(
        initiator,
        Base64SHA256(peripheral),
        receivedTime,
        organization,
        version,
        Base64.encodeToString(peerPubKey, Base64.NO_WRAP),
        0,
        rssi
    ) {

    }

    companion object {
        // we compute a 6 bytes hash from the mac address.
        // collision is about 2^(N/2) = 2^24 ~= 16,777,216 rows before we may collide.
        // totally acceptable for our use case
        fun Base64SHA256(address: String): String {
            val d = SHA256.Digest()
            d.update(address.toByteArray())
            return Base64.encodeToString(d.digest().sliceArray(0..5), Base64.NO_WRAP)
        }
    }

}

