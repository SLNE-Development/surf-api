package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class TimestampSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(TimestampSerializer::class) Instant,
        @ProtoNumber(2) val list: List<@Serializable(TimestampSerializer::class) Instant> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(TimestampSerializer::class) Instant> = emptyMap(),
        @ProtoNumber(4) val nullable: @Serializable(TimestampSerializer::class) Instant? = null,
    )

    @Test
    fun `descriptor is the google Timestamp message`() {
        assertMessageDescriptor(
            TimestampSerializer.descriptor,
            "google.protobuf.Timestamp",
            "seconds" to 1,
            "nanos" to 2
        )
    }

    @Test
    fun `top-level wire bytes`() {
        val value = Instant.ofEpochSecond(5, 7)

        assertEquals("08051007", encodeHex(TimestampSerializer, value))
        assertEquals(value, decodeHex(TimestampSerializer, "08051007"))
    }

    @Test
    fun `epoch is an empty message`() {
        assertEquals("", encodeHex(TimestampSerializer, Instant.EPOCH))
        assertEquals(Instant.EPOCH, decodeHex(TimestampSerializer, ""))
    }

    @Test
    fun `negative seconds keep non-negative nanos`() {
        val value = Instant.ofEpochSecond(-1, 500_000_000)
        val hex = "08ffffffffffffffffff011080cab5ee01"

        assertEquals(hex, encodeHex(TimestampSerializer, value))
        assertEquals(value, decodeHex(TimestampSerializer, hex))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val value = Holder(
            single = Instant.ofEpochSecond(5, 7),
            list = listOf(Instant.EPOCH, Instant.ofEpochSecond(1)),
            map = mapOf("a" to Instant.EPOCH, "b" to Instant.ofEpochSecond(2, 3)),
            nullable = Instant.EPOCH,
        )
        val hex = "0a04080510071200120208011a050a016112001a090a01621204080210032200"

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent fields default to zero`() {
        assertEquals(Instant.ofEpochSecond(0, 7), decodeHex(TimestampSerializer, "1007"))
        assertEquals(Instant.ofEpochSecond(5), decodeHex(TimestampSerializer, "0805"))
    }

    @Test
    fun `empty nested timestamp decodes to the epoch`() {
        assertEquals(Holder(Instant.EPOCH), decodeHex(Holder.serializer(), "0a00"))
    }

    @Test
    fun `nanos outside 0 to 999999999 are rejected`() {
        assertInvalid { decodeHex(TimestampSerializer, "108094ebdc03") }
        assertInvalid { decodeHex(TimestampSerializer, "10ffffffffffffffffff01") }
    }

    @Test
    fun `seconds outside the Instant range are rejected`() {
        assertInvalid { decodeHex(TimestampSerializer, "08ffffffffffffffff7f") }
    }
}
