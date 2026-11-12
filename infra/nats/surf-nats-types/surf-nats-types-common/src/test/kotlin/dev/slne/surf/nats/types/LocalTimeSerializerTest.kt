package dev.slne.surf.nats.types

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalTimeSerializerTest {

    @Test
    fun `descriptor is the surf LocalTime message`() {
        assertMessageDescriptor(
            LocalTimeSerializer.descriptor,
            "surf.types.v1.LocalTime",
            "hour" to 1, "minute" to 2, "second" to 3, "nanos" to 4,
        )
    }

    @Test
    fun `top-level wire bytes`() {
        val value = LocalTime.of(12, 30, 15, 500_000_000)
        val hex = "080c101e180f2080cab5ee01"

        assertEquals(hex, encodeHex(LocalTimeSerializer, value))
        assertEquals(value, decodeHex(LocalTimeSerializer, hex))
    }

    @Test
    fun `midnight is the empty message`() {
        assertEquals("", encodeHex(LocalTimeSerializer, LocalTime.MIDNIGHT))
        assertEquals(LocalTime.MIDNIGHT, decodeHex(LocalTimeSerializer, ""))
    }

    @Test
    fun `invalid time is rejected`() {
        assertInvalid { decodeHex(LocalTimeSerializer, "0818") } // hour 24
        assertInvalid { decodeHex(LocalTimeSerializer, "103c") } // minute 60
    }
}
