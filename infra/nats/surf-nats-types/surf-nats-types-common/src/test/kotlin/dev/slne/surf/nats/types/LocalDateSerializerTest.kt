package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class LocalDateSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(LocalDateSerializer::class) LocalDate,
        @ProtoNumber(2) val nullable: @Serializable(LocalDateSerializer::class) LocalDate? = null,
    )

    private val value = LocalDate.of(2026, 11, 3)
    private val hex = "08ea0f100b1803"

    @Test
    fun `descriptor is the surf LocalDate message`() {
        assertMessageDescriptor(
            LocalDateSerializer.descriptor,
            "surf.types.v1.LocalDate",
            "year" to 1, "month" to 2, "day" to 3,
        )
    }

    @Test
    fun `top-level wire bytes`() {
        assertEquals(hex, encodeHex(LocalDateSerializer, value))
        assertEquals(value, decodeHex(LocalDateSerializer, hex))
    }

    @Test
    fun `nested and nullable fields`() {
        val holder = Holder(value, value)
        val nested = "0a07$hex" + "1207$hex"

        assertEquals(nested, encodeHex(Holder.serializer(), holder))
        assertEquals(holder, decodeHex(Holder.serializer(), nested))
        assertEquals(Holder(value), decodeHex(Holder.serializer(), "0a07$hex"))
    }

    @Test
    fun `absent fields are missing`() {
        assertMissingField("surf.types.v1.LocalDate", "year", "month", "day") { decodeHex(LocalDateSerializer, "") }
    }

    @Test
    fun `invalid date is rejected`() {
        assertInvalid { decodeHex(LocalDateSerializer, "08ea0f1002181e") } // 2026-02-30
        assertInvalid { decodeHex(LocalDateSerializer, "08ea0f100d1801") } // month 13
    }
}
