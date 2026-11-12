package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.OffsetDateTime
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class OffsetDateTimeSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(OffsetDateTimeSerializer::class) OffsetDateTime,
        @ProtoNumber(2) val list: List<@Serializable(OffsetDateTimeSerializer::class) OffsetDateTime> = emptyList(),
    )

    private val value = OffsetDateTime.of(2026, 11, 3, 12, 30, 15, 500_000_000, ZoneOffset.ofHours(1))
    private val hex = "08c78ba7d7061080cab5ee0118901c"

    @Test
    fun `descriptor is the surf OffsetDateTime message`() {
        assertMessageDescriptor(
            OffsetDateTimeSerializer.descriptor,
            "surf.types.v1.OffsetDateTime",
            "seconds" to 1, "nanos" to 2, "offsetSeconds" to 3,
        )
    }

    @Test
    fun `top-level wire bytes keep instant and offset`() {
        assertEquals(hex, encodeHex(OffsetDateTimeSerializer, value))
        assertEquals(value, decodeHex(OffsetDateTimeSerializer, hex))
    }

    @Test
    fun `zero nanos are omitted and a zero offset is written`() {
        val utc = OffsetDateTime.of(2026, 11, 3, 11, 30, 15, 0, ZoneOffset.UTC)

        assertEquals("08c78ba7d7061800", encodeHex(OffsetDateTimeSerializer, utc))
        assertEquals(utc, decodeHex(OffsetDateTimeSerializer, "08c78ba7d7061800"))
    }

    @Test
    fun `negative offset round trips`() {
        val value = OffsetDateTime.of(2026, 11, 3, 6, 30, 15, 0, ZoneOffset.ofHours(-5))
        val hex = "08c78ba7d70618b0f3feffffffffffff01"

        assertEquals(hex, encodeHex(OffsetDateTimeSerializer, value))
        assertEquals(value, decodeHex(OffsetDateTimeSerializer, hex))
    }

    @Test
    fun `nested field and list elements`() {
        val holder = Holder(single = value, list = listOf(value))
        val nested = "0a0f$hex" + "120f$hex"

        assertEquals(nested, encodeHex(Holder.serializer(), holder))
        assertEquals(holder, decodeHex(Holder.serializer(), nested))
    }

    @Test
    fun `absent seconds or offset is a missing field`() {
        assertMissingField("surf.types.v1.OffsetDateTime", "seconds", "offsetSeconds") {
            decodeHex(OffsetDateTimeSerializer, "")
        }
        assertMissingField("surf.types.v1.OffsetDateTime", "offsetSeconds") {
            decodeHex(OffsetDateTimeSerializer, "08c78ba7d706")
        }
    }

    @Test
    fun `invalid nanos and offsets are rejected`() {
        assertInvalid { decodeHex(OffsetDateTimeSerializer, "08c78ba7d70610ffffffffffffffffff011800") } // nanos -1
        assertInvalid { decodeHex(OffsetDateTimeSerializer, "08c78ba7d70618c0b204") } // offset 72,000 s (+20:00)
    }
}
