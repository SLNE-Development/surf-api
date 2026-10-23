package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import kotlin.test.Test
import kotlin.test.assertEquals

class ComponentSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(ComponentSerializer::class) Component,
        @ProtoNumber(2) val list: List<@Serializable(ComponentSerializer::class) Component> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(ComponentSerializer::class) Component> = emptyMap(),
    )

    private val red = Component.text("hi", NamedTextColor.RED)
    private val redHex = "0a1b" + "7b22636f6c6f72223a22726564222c2274657874223a226869227d" // {"color":"red","text":"hi"}

    @Test
    fun `descriptor is the surf Component message`() {
        assertMessageDescriptor(ComponentSerializer.descriptor, "surf.types.v1.Component", "json" to 1)
    }

    @Test
    fun `top-level wire bytes carry the gson json`() {
        assertEquals(redHex, encodeHex(ComponentSerializer, red))
        assertEquals(red, decodeHex(ComponentSerializer, redHex))
    }

    @Test
    fun `plain text uses the compact json form`() {
        assertEquals("0a0422686922", encodeHex(ComponentSerializer, Component.text("hi"))) // "hi"
        assertEquals(Component.text("hi"), decodeHex(ComponentSerializer, "0a0422686922"))
    }

    @Test
    fun `components are compacted before encoding`() {
        val nested = Component.text().append(Component.text("a")).build()

        assertEquals(encodeHex(ComponentSerializer, Component.text("a")), encodeHex(ComponentSerializer, nested))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val value = Holder(
            single = red,
            list = listOf(Component.text("hi")),
            map = mapOf("k" to Component.text("a")),
        )
        val hex = "0a1d" + redHex +
                "12060a0422686922" +
                "1a0a0a016b12050a03226122"

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent json is a missing field`() {
        assertMissingField("surf.types.v1.Component", "json") { decodeHex(ComponentSerializer, "") }
        assertMissingField("surf.types.v1.Component", "json") { decodeHex(Holder.serializer(), "0a00") }
    }

    @Test
    fun `invalid json is rejected`() {
        assertInvalid { decodeHex(ComponentSerializer, "0a017b") } // "{"
    }
}
