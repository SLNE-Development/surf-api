package dev.slne.surf.nats.types

import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ZonedDateTimeSerializerTest {

    private val berlin = ZoneId.of("Europe/Berlin")
    private val value = ZonedDateTime.of(2026, 11, 3, 12, 30, 15, 500_000_000, berlin)
    private val hex = "0a0f08c78ba7d7061080cab5ee0118901c" + "120d4575726f70652f4265726c696e"

    @Test
    fun `descriptor is the surf ZonedDateTime message`() {
        assertMessageDescriptor(
            ZonedDateTimeSerializer.descriptor,
            "surf.types.v1.ZonedDateTime",
            "dateTime" to 1, "zoneId" to 2,
        )
    }

    @Test
    fun `top-level wire bytes keep instant, offset and zone`() {
        assertEquals(hex, encodeHex(ZonedDateTimeSerializer, value))
        assertEquals(value, decodeHex(ZonedDateTimeSerializer, hex))
    }

    @Test
    fun `offset that contradicts the zone rules keeps the instant`() {
        // 12:30:15.5+01:00 with zone "Europe/Berlin", but the offset written as +02:00.
        val hex = "0a0f08c78ba7d7061080cab5ee0118a038" + "120d4575726f70652f4265726c696e"
        val decoded = decodeHex(ZonedDateTimeSerializer, hex)

        assertEquals(value.toInstant(), decoded.toInstant())
        assertEquals(ZoneOffset.ofHours(1), decoded.offset)
        assertEquals(berlin, decoded.zone)
    }

    @Test
    fun `absent fields are missing`() {
        assertMissingField("surf.types.v1.ZonedDateTime", "dateTime", "zoneId") {
            decodeHex(ZonedDateTimeSerializer, "")
        }
    }

    @Test
    fun `unknown zone is rejected`() {
        assertInvalid { decodeHex(ZonedDateTimeSerializer, "0a0f08c78ba7d7061080cab5ee0118901c" + "1203416263") } // "Abc"
    }
}
