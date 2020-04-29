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

package world.coalition.whisper.id

import android.util.Base64
import kotlinx.serialization.Serializable

/**
 * @author Lucien Loiseau on 28/03/20.
 */
@Serializable
data class SessionKeyParam(
    val SecretKey: ByteArray,
    val TimeReferenceSec: Long,
    val ExpireAfterSec: Int,
    val TimeStepSec: Int,
    val KdfId: String
) {

    override fun toString(): String {
        return "s=key=${Base64.encodeToString(SecretKey, Base64.NO_WRAP)} ref=$TimeReferenceSec expire=$ExpireAfterSec step=$TimeStepSec kdfid=$KdfId"
    }

    // auto generated
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SessionKeyParam

        if (!SecretKey.contentEquals(other.SecretKey)) return false
        if (TimeReferenceSec != other.TimeReferenceSec) return false
        if (ExpireAfterSec != other.ExpireAfterSec) return false
        if (TimeStepSec != other.TimeStepSec) return false
        if (KdfId != other.KdfId) return false

        return true
    }

    // auto generated
    override fun hashCode(): Int {
        var result = SecretKey.contentHashCode()
        result = 31 * result + TimeReferenceSec.hashCode()
        result = 31 * result + ExpireAfterSec
        result = 31 * result + TimeStepSec
        result = 31 * result + KdfId.hashCode()
        return result
    }
}