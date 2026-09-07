package de.loxitation.pht.ble

import java.time.LocalDateTime
import java.util.Locale

/**
 * Decodes the standard Bluetooth SIG "Blood Pressure Measurement" characteristic
 * (0x2A35, part of the Blood Pressure Service 0x1810).
 *
 * Wire format (little-endian):
 * - byte 0: flags
 *     bit 0: units (0 = mmHg, 1 = kPa)
 *     bit 1: time stamp present
 *     bit 2: pulse rate present
 *     bit 3: user id present
 *     bit 4: measurement status present
 * - SFLOAT systolic, SFLOAT diastolic, SFLOAT mean arterial pressure (always present)
 * - [if bit 1] time stamp: uint16 year, uint8 month, day, hours, minutes, seconds
 * - [if bit 2] SFLOAT pulse rate
 * - [if bit 3] uint8 user id
 * - [if bit 4] uint16 measurement status bitfield
 *
 * Verified against the Beurer BM48 sample from Phase 1:
 * `16 96 00 5b 00 00 00 ea 07 09 06 14 19 00 59 00 04 40`
 * decodes to systolic 150, diastolic 91, pulse 89, time 2026-09-06T20:25:00,
 * measurementStatusRaw 16388.
 */
object BloodPressureMeasurementParser {

    private const val FLAG_KPA = 0x01
    private const val FLAG_TIMESTAMP = 0x02
    private const val FLAG_PULSE = 0x04
    private const val FLAG_USER_ID = 0x08
    private const val FLAG_MEASUREMENT_STATUS = 0x10

    fun parse(rawBytes: ByteArray): ParsedMeasurement.BloodPressure? {
        if (rawBytes.size < 7) return null // flags + 3 SFLOATs is the minimum possible payload

        var offset = 0
        val flags = rawBytes[offset].toInt() and 0xFF
        offset += 1

        val isKPa = flags and FLAG_KPA != 0
        val hasTimestamp = flags and FLAG_TIMESTAMP != 0
        val hasPulse = flags and FLAG_PULSE != 0
        val hasUserId = flags and FLAG_USER_ID != 0
        val hasMeasurementStatus = flags and FLAG_MEASUREMENT_STATUS != 0

        val systolic = readSFloat(rawBytes, offset) ?: return null
        offset += 2
        val diastolic = readSFloat(rawBytes, offset) ?: return null
        offset += 2
        val meanArterialPressure = readSFloat(rawBytes, offset)
        offset += 2

        var timestamp: LocalDateTime? = null
        if (hasTimestamp) {
            if (rawBytes.size < offset + 7) return null
            val year = readUInt16(rawBytes, offset)
            val month = rawBytes[offset + 2].toInt() and 0xFF
            val day = rawBytes[offset + 3].toInt() and 0xFF
            val hour = rawBytes[offset + 4].toInt() and 0xFF
            val minute = rawBytes[offset + 5].toInt() and 0xFF
            val second = rawBytes[offset + 6].toInt() and 0xFF
            offset += 7
            timestamp = runCatching {
                LocalDateTime.of(year, month, day, hour, minute, second)
            }.getOrNull()
        }

        var pulse: Int? = null
        if (hasPulse) {
            if (rawBytes.size < offset + 2) return null
            pulse = readSFloat(rawBytes, offset)?.toInt()
            offset += 2
        }

        if (hasUserId) {
            offset += 1 // user id itself is not currently modeled server-side
        }

        var measurementStatusRaw: Int? = null
        if (hasMeasurementStatus) {
            if (rawBytes.size < offset + 2) return null
            measurementStatusRaw = readUInt16(rawBytes, offset)
            offset += 2
        }

        return ParsedMeasurement.BloodPressure(
            systolic = systolic,
            diastolic = diastolic,
            meanArterialPressure = meanArterialPressure,
            pulse = pulse,
            timestamp = timestamp,
            measurementStatusRaw = measurementStatusRaw,
            isKPa = isKPa,
            rawHex = rawBytes.toHex(),
        )
    }

    private fun readUInt16(bytes: ByteArray, offset: Int): Int {
        val lo = bytes[offset].toInt() and 0xFF
        val hi = bytes[offset + 1].toInt() and 0xFF
        return (hi shl 8) or lo
    }

    /**
     * Decodes an IEEE-11073 16-bit SFLOAT: 4-bit signed exponent (bits 15-12),
     * 12-bit signed mantissa (bits 11-0). Reserved special mantissa values
     * (NaN, +INFINITY, -INFINITY, NRes) decode to null.
     */
    private fun readSFloat(bytes: ByteArray, offset: Int): Float? {
        val raw = readUInt16(bytes, offset)
        var mantissa = raw and 0x0FFF
        val exponent = (raw shr 12) and 0x0F

        // Special reserved mantissa values per IEEE 11073-20601 SFLOAT.
        if (mantissa in 0x07FE..0x0802) return null

        if (mantissa > 0x07FF) {
            mantissa -= 0x1000 // sign-extend 12-bit two's complement
        }
        val signedExponent = if (exponent > 0x07) exponent - 0x10 else exponent

        return (mantissa * Math.pow(10.0, signedExponent.toDouble())).toFloat()
    }

    private fun ByteArray.toHex(): String =
        joinToString(" ") { String.format(Locale.ROOT, "%02x", it) }
}
