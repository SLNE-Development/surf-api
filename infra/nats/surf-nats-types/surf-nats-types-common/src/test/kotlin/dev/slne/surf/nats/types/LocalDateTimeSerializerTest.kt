package dev.slne.surf.nats.types

import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateTimeSerializerTest {

    @Test
    fun `descriptor is the surf LocalDateTime message`() {
        assertMessageDescriptor(
            LocalDateTimeSerializer.descriptor,
            "surf.types.v1.LocalDateTime",
            "date" to 1, "time" to 2,
        )
    }

    @Test
    fun `top-level wire bytes`() {
        val value = LocalDateTime.of(2026, 11, 3, 12, 30)
        val hex = "0a0708ea0f100b18031204080c101e"

        assertEquals(hex, encodeHex(LocalDateTimeSerializer, value))
        assertEquals(value, decodeHex(LocalDateTimeSerializer, hex))
    }

    @Test
    fun `midnight keeps an empty time message`() {
        val value = LocalDateTime.of(2026, 11, 3, 0, 0)
        val hex = "0a0708ea0f100b18031200"

        assertEquals(hex, encodeHex(LocalDateTimeSerializer, value))
        assertEquals(value, decodeHex(LocalDateTimeSerializer, hex))
    }

    @Test
    fun `absent date or time is a missing field`() {
        assertMissingField("surf.types.v1.LocalDateTime", "date", "time") { decodeHex(LocalDateTimeSerializer, "") }
        assertMissingField("surf.types.v1.LocalDateTime", "time") {
            decodeHex(LocalDateTimeSerializer, "0a0708ea0f100b1803")
        }
    }
}
