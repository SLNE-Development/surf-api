package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.key.Key
import kotlin.test.Test
import kotlin.test.assertEquals

class KeySerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(KeySerializer::class) Key,
        @ProtoNumber(2) val list: List<@Serializable(KeySerializer::class) Key> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(KeySerializer::class) Key> = emptyMap(),
    )

    @Test
    fun `descriptor is the surf Key message`() {
        assertMessageDescriptor(KeySerializer.descriptor, "surf.types.v1.Key", "value" to 1)
    }

    @Test
    fun `top-level wire bytes`() {
        val value = Key.key("surf", "test")
        val hex = "0a09737572663a74657374"

        assertEquals(hex, encodeHex(KeySerializer, value))
        assertEquals(value, decodeHex(KeySerializer, hex))
    }

    @Test
    fun `value without namespace uses the minecraft namespace`() {
        assertEquals(Key.key("minecraft", "stone"), decodeHex(KeySerializer, "0a0573746f6e65"))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val value = Holder(
            single = Key.key("surf", "test"),
            list = listOf(Key.key("minecraft", "stone")),
            map = mapOf("k" to Key.key("surf", "other")),
        )
        val hex = "0a0b0a09737572663a74657374" +
                "12110a0f6d696e6563726166743a73746f6e65" +
                "1a110a016b120c0a0a737572663a6f74686572"

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `absent value is a missing field`() {
        assertMissingField("surf.types.v1.Key", "value") { decodeHex(KeySerializer, "") }
        assertMissingField("surf.types.v1.Key", "value") { decodeHex(Holder.serializer(), "0a00") }
    }

    @Test
    fun `invalid key is rejected`() {
        assertInvalid { decodeHex(KeySerializer, "0a07426164204b6579") } // "Bad Key"
    }
}
