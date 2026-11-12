package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class TextColorSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(TextColorSerializer::class) TextColor,
        @ProtoNumber(2) val nullable: @Serializable(TextColorSerializer::class) TextColor? = null,
    )

    @Test
    fun `descriptor is the surf TextColor message`() {
        assertMessageDescriptor(TextColorSerializer.descriptor, "surf.types.v1.TextColor", "rgb" to 1)
    }

    @Test
    fun `named color comes back as the same constant`() {
        assertEquals("08d5aafd07", encodeHex(TextColorSerializer, NamedTextColor.RED))
        assertSame(NamedTextColor.RED, decodeHex(TextColorSerializer, "08d5aafd07"))
    }

    @Test
    fun `custom color round trips`() {
        val value = TextColor.color(0xFF6B6B)

        assertEquals("08ebd6fd07", encodeHex(TextColorSerializer, value))
        assertEquals(value, decodeHex(TextColorSerializer, "08ebd6fd07"))
    }

    @Test
    fun `black is written explicitly`() {
        assertEquals("0800", encodeHex(TextColorSerializer, NamedTextColor.BLACK))
        assertSame(NamedTextColor.BLACK, decodeHex(TextColorSerializer, "0800"))
    }

    @Test
    fun `nested and nullable fields`() {
        val holder = Holder(NamedTextColor.RED, TextColor.color(0xFF6B6B))
        val hex = "0a0508d5aafd07" + "120508ebd6fd07"

        assertEquals(hex, encodeHex(Holder.serializer(), holder))
        assertEquals(holder, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent rgb is a missing field`() {
        assertMissingField("surf.types.v1.TextColor", "rgb") { decodeHex(TextColorSerializer, "") }
    }

    @Test
    fun `rgb outside 24 bits is rejected`() {
        assertInvalid { decodeHex(TextColorSerializer, "0880808008") } // 0x1000000
        assertInvalid { decodeHex(TextColorSerializer, "08ffffffffffffffffff01") } // -1
    }
}
