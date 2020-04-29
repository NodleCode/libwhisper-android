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
import world.coalition.whisper.agathe.TIDPayload
import world.coalition.whisper.agathe.TIDWithChallengePayload

/**
 * @author Lucien Loiseau on 31/03/20.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = PeerTid::class,
            parentColumns = ["row_id"],
            childColumns = ["adv_tid_rowid"],
            onDelete = ForeignKey.CASCADE
        )]
)
data class PeerContactEvent(
    @ColumnInfo(name = "peripheral_hash", index = true) var peripheralId: String, // hash of peripheral address
    @ColumnInfo(name = "connect_time_ms") val connectTimeMillis: Long,         // receive timestamp
    @ColumnInfo(name = "organization") val organization: String,          // organisation
    @ColumnInfo(name = "adv_v") val version: Int,                         // advertised version
    @ColumnInfo(name = "adv_tid_rowid", index = true) var advPeerTidRowId: Long,    // advertised id
    @ColumnInfo(name = "adv_challenge") val advertisedChallenge: String,  // advertised timestamp
    @ColumnInfo(name = "adv_hmac") val advertisedHmac: String,            // advertised hmac  (whisper specific)
    @ColumnInfo(name = "rssi") val rssi: Int,                             // received rssi
    @ColumnInfo(name = "metadata") val metadata: String                   // metadata (tracetogether phone model)
) {
    @NonNull
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "row_id", index = true)
    var id: Long = 0

    @Ignore
    var peerTid: String = ""                                   // peer's temporary id

    constructor(
        peripheral: String, receivedTime: Long, organization: String, version: Int,
        peerId: String, advertisedChallenge: String, advertisedHmac: String,
        rssi: Int, metadata: String
    ) : this(
        Base64SHA256(peripheral),
        receivedTime,
        organization,
        version,
        0,
        advertisedChallenge,
        advertisedHmac,
        rssi,
        metadata
    ) {
        this.peerTid = peerId
    }

    companion object {
        fun fromTIDPayload(
            p: TIDPayload,
            peripheral: String,
            rssi: Int,
            receivedTime: Long
        ): PeerContactEvent {
            val orgStr = when (p.organization) {
                0x01 -> "Coalition"
                else -> "Unknown(${p.organization})"
            }

            return PeerContactEvent(
                peripheral,
                receivedTime,
                orgStr,
                p.version,
                Base64.encodeToString(p.temporaryId, Base64.NO_WRAP),
                Base64.encodeToString(p.challenge, Base64.NO_WRAP),
                Base64.encodeToString(p.hmac, Base64.NO_WRAP),
                rssi,
                ""
            )
        }

        fun fromTIDWithChallengePayload(
            p: TIDWithChallengePayload,
            peripheral: String,
            rssi: Int,
            receivedTime: Long
        ): PeerContactEvent {
            val orgStr = when (p.organization) {
                0x01 -> "Coalition"
                else -> "Unknown(${p.organization})"
            }

            return PeerContactEvent(
                peripheral,
                receivedTime,
                orgStr,
                p.version,
                Base64.encodeToString(p.temporaryId, Base64.NO_WRAP),
                Base64.encodeToString(p.challenge1, Base64.NO_WRAP),
                Base64.encodeToString(p.hmac, Base64.NO_WRAP),
                rssi,
                ""
            )
        }

        // we compute a 6 bytes hash from the mac address.
        // collision is roughly about 2^(N/2) = 2^24 ~= 16,777,216 rows
        // totally acceptable for our use case
        fun Base64SHA256(address: String): String {
            val d = SHA256.Digest()
            d.update(address.toByteArray())
            return Base64.encodeToString(d.digest().sliceArray(0..5), Base64.NO_WRAP)
        }
    }

}
