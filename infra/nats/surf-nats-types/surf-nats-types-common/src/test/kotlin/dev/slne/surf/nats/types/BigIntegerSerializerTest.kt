package dev.slne.surf.nats.types

import java.math.BigInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class BigIntegerSerializerTest {

    @Test
    fun `descriptor is the surf BigInteger message`() {
        assertMessageDescriptor(BigIntegerSerializer.descriptor, "surf.types.v1.BigInteger", "value" to 1)
    }

    @Test
    fun `top-level wire bytes are decimal digits`() {
        val value = BigInteger("-123456789012345678901234567890")
        val hex = "0a1f2d313233343536373839303132333435363738393031323334353637383930"

        assertEquals(hex, encodeHex(BigIntegerSerializer, value))
        assertEquals(value, decodeHex(BigIntegerSerializer, hex))
    }

    @Test
    fun `absent value is a missing field`() {
        assertMissingField("surf.types.v1.BigInteger", "value") { decodeHex(BigIntegerSerializer, "") }
    }

    @Test
    fun `invalid integer is rejected`() {
        assertInvalid { decodeHex(BigIntegerSerializer, "0a03312e35") } // "1.5"
    }
}
