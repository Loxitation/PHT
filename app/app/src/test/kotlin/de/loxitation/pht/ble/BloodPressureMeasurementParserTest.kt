package de.loxitation.pht.ble

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

/**
 * Regression test for the exact Beurer BM48 sample captured in Phase 1's Python prototype:
 * `16 96 00 5b 00 00 00 ea 07 09 06 14 19 00 59 00 04 40`
 * -> systolic 150, diastolic 91, pulse 89, time 2026-09-06T20:25:00, status 16388.
 */
class BloodPressureMeasurementParserTest {

    private fun hexToBytes(hex: String): ByteArray =
        hex.split(" ").map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun `parses the real BM48 sample correctly`() {
        val raw = hexToBytes("16 96 00 5b 00 00 00 ea 07 09 06 14 19 00 59 00 04 40")

        val result = BloodPressureMeasurementParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals(150.0f, result.systolic)
        assertEquals(91.0f, result.diastolic)
        assertEquals(0.0f, result.meanArterialPressure)
        assertEquals(89, result.pulse)
        assertEquals(LocalDateTime.of(2026, 9, 6, 20, 25, 0), result.timestamp)
        assertEquals(16388, result.measurementStatusRaw)
        assertEquals(false, result.isKPa)
    }

    @Test
    fun `returns null for too-short payloads`() {
        val raw = hexToBytes("16 96 00")
        assertNull(BloodPressureMeasurementParser.parse(raw))
    }

    @Test
    fun `parses minimal payload without timestamp, pulse, or status`() {
        // flags = 0x00 -> mmHg, no timestamp, no pulse, no user id, no status
        val raw = hexToBytes("00 78 00 50 00 00 00")

        val result = BloodPressureMeasurementParser.parse(raw)

        assertNotNull(result)
        result!!
        assertEquals(120.0f, result.systolic)
        assertEquals(80.0f, result.diastolic)
        assertNull(result.timestamp)
        assertNull(result.pulse)
        assertNull(result.measurementStatusRaw)
    }
}
