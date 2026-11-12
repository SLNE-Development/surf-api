package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

class DurationSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(DurationSerializer::class) Duration,
        @ProtoNumber(2) val list: List<@Serializable(DurationSerializer::class) Duration> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(DurationSerializer::class) Duration> = emptyMap(),
        @ProtoNumber(4) val nullable: @Serializable(DurationSerializer::class) Duration? = null,
    )

    @Test
    fun `descriptor is the google Duration message`() {
        assertMessageDescriptor(
            DurationSerializer.descriptor,
            "google.protobuf.Duration",
            "seconds" to 1,
            "nanos" to 2
        )
    }

    @Test
    fun `positive duration`() {
        val hex = "08011080cab5ee01"

        assertEquals(hex, encodeHex(DurationSerializer, 1500.milliseconds))
        assertEquals(1500.milliseconds, decodeHex(DurationSerializer, hex))
    }

    @Test
    fun `negative duration has negative seconds and nanos`() {
        val value = (-1500).milliseconds
        val hex = "08ffffffffffffffffff011080b6ca91feffffffff01"

        assertEquals(-1L to -500_000_000, value.toComponents { seconds, nanos -> seconds to nanos })
        assertEquals(hex, encodeHex(DurationSerializer, value))
        assertEquals(value, decodeHex(DurationSerializer, hex))
    }

    @Test
    fun `negative sub-second duration has only negative nanos`() {
        val hex = "10fbffffffffffffffff01"

        assertEquals(hex, encodeHex(DurationSerializer, (-5).nanoseconds))
        assertEquals((-5).nanoseconds, decodeHex(DurationSerializer, hex))
    }

    @Test
    fun `zero is an empty message`() {
        assertEquals("", encodeHex(DurationSerializer, Duration.ZERO))
        assertEquals(Duration.ZERO, decodeHex(DurationSerializer, ""))
    }

    @Test
    fun `range limits round trip`() {
        val max = 315_576_000_000.seconds
        val min = (-315_576_000_000).seconds

        assertEquals("0880bcaece9709", encodeHex(DurationSerializer, max))
        assertEquals(max, decodeHex(DurationSerializer, "0880bcaece9709"))
        assertEquals("0880c4d1b1e8f6ffffff01", encodeHex(DurationSerializer, min))
        assertEquals(min, decodeHex(DurationSerializer, "0880c4d1b1e8f6ffffff01"))
    }

    @Test
    fun `durations outside the google Duration range are rejected on encode`() {
        assertInvalid { encodeHex(DurationSerializer, 315_576_000_001.seconds) }
        assertInvalid { encodeHex(DurationSerializer, (-315_576_000_001).seconds) }
        assertInvalid { encodeHex(DurationSerializer, Duration.INFINITE) }
        assertInvalid { encodeHex(DurationSerializer, -Duration.INFINITE) }
    }

    @Test
    fun `invalid wire values are rejected on decode`() {
        assertInvalid { decodeHex(DurationSerializer, "0881bcaece9709") } // seconds 315576000001
        assertInvalid { decodeHex(DurationSerializer, "108094ebdc03") } // nanos 1000000000
        assertInvalid { decodeHex(DurationSerializer, "080110ffffffffffffffffff01") } // 1 s, -1 ns
        assertInvalid { decodeHex(DurationSerializer, "08ffffffffffffffffff011001") } // -1 s, 1 ns
    }

    @Test
    fun `nested field, list elements and map values`() {
        val value = Holder(
            single = 2.seconds,
            list = listOf(Duration.ZERO, (-1500).milliseconds),
            map = mapOf("a" to 1500.milliseconds),
        )
        val hex = "0a0208021200121608ffffffffffffffffff011080b6ca91feffffffff011a0d0a0161120808011080cab5ee01"

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `empty nested duration decodes to zero`() {
        assertEquals(Holder(Duration.ZERO), decodeHex(Holder.serializer(), "0a00"))
    }
}
