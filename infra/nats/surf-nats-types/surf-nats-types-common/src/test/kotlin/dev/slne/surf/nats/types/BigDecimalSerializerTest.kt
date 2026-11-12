package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals

class BigDecimalSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val list: List<@Serializable(BigDecimalSerializer::class) BigDecimal> = emptyList(),
    )

    @Test
    fun `descriptor is the surf BigDecimal message`() {
        assertMessageDescriptor(BigDecimalSerializer.descriptor, "surf.types.v1.BigDecimal", "value" to 1)
    }

    @Test
    fun `wire bytes keep the scale`() {
        val value = BigDecimal("12.50")

        assertEquals("0a0531322e3530", encodeHex(BigDecimalSerializer, value))
        val decoded = decodeHex(BigDecimalSerializer, "0a0531322e3530")
        assertEquals(value, decoded)
        assertEquals(2, decoded.scale())
    }

    @Test
    fun `negative scale and small values round trip`() {
        assertEquals("0a0431452b33", encodeHex(BigDecimalSerializer, BigDecimal("1E+3")))
        assertEquals(BigDecimal("1E+3"), decodeHex(BigDecimalSerializer, "0a0431452b33"))
        assertEquals(BigDecimal("-0.001"), decodeHex(BigDecimalSerializer, "0a062d302e303031"))
    }

    @Test
    fun `list elements`() {
        val holder = Holder(listOf(BigDecimal("12.50"), BigDecimal("-0.001")))
        val hex = "0a070a0531322e3530" + "0a080a062d302e303031"

        assertEquals(hex, encodeHex(Holder.serializer(), holder))
        assertEquals(holder, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent value is a missing field`() {
        assertMissingField("surf.types.v1.BigDecimal", "value") { decodeHex(BigDecimalSerializer, "") }
    }

    @Test
    fun `invalid decimal is rejected`() {
        assertInvalid { decodeHex(BigDecimalSerializer, "0a03312c35") } // "1,5"
    }
}
