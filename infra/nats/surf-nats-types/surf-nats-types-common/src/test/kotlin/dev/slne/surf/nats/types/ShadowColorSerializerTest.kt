package dev.slne.surf.nats.types

import net.kyori.adventure.text.format.ShadowColor
import kotlin.test.Test
import kotlin.test.assertEquals

class ShadowColorSerializerTest {

    @Test
    fun `descriptor is the surf ShadowColor message`() {
        assertMessageDescriptor(ShadowColorSerializer.descriptor, "surf.types.v1.ShadowColor", "argb" to 1)
    }

    @Test
    fun `argb is a little-endian fixed32`() {
        val value = ShadowColor.shadowColor(0x11, 0x22, 0x33, 0x44)

        assertEquals("0d33221144", encodeHex(ShadowColorSerializer, value))
        assertEquals(value, decodeHex(ShadowColorSerializer, "0d33221144"))
    }

    @Test
    fun `opaque black keeps the high bit`() {
        val value = ShadowColor.shadowColor(0xFF000000.toInt())

        assertEquals("0d000000ff", encodeHex(ShadowColorSerializer, value))
        assertEquals(value, decodeHex(ShadowColorSerializer, "0d000000ff"))
    }

    @Test
    fun `transparent is written explicitly`() {
        assertEquals("0d00000000", encodeHex(ShadowColorSerializer, ShadowColor.none()))
        assertEquals(ShadowColor.none(), decodeHex(ShadowColorSerializer, "0d00000000"))
    }

    @Test
    fun `absent argb is a missing field`() {
        assertMissingField("surf.types.v1.ShadowColor", "argb") { decodeHex(ShadowColorSerializer, "") }
    }
}
