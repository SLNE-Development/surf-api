package dev.slne.surf.nats.types

import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals

class UuidSerializerTest {

    @Serializable
    private data class Holder(
        @ProtoNumber(1) val single: @Serializable(UuidSerializer::class) UUID,
        @ProtoNumber(2) val list: List<@Serializable(UuidSerializer::class) UUID> = emptyList(),
        @ProtoNumber(3) val map: Map<String, @Serializable(UuidSerializer::class) UUID> = emptyMap(),
        @ProtoNumber(4) val nullable: @Serializable(UuidSerializer::class) UUID? = null,
    )

    private val a = UUID.fromString("00112233-4455-6677-8899-aabbccddeeff")
    private val b = UUID.fromString("ffeeddcc-bbaa-9988-7766-554433221100")

    @Test
    fun `descriptor is the surf Uuid message`() {
        assertMessageDescriptor(UuidSerializer.descriptor, "surf.types.v1.Uuid", "value" to 1)
    }

    @Test
    fun `top-level wire bytes are big-endian msb then lsb`() {
        val hex = "0a1000112233445566778899aabbccddeeff"

        assertEquals(hex, encodeHex(UuidSerializer, a))
        assertEquals(a, decodeHex(UuidSerializer, hex))
    }

    @Test
    fun `nested field, list elements and map values`() {
        val value = Holder(single = a, list = listOf(a, b), map = mapOf("k" to b))
        val hex = "0a120a1000112233445566778899aabbccddeeff" +
                "12120a1000112233445566778899aabbccddeeff" +
                "12120a10ffeeddccbbaa99887766554433221100" +
                "1a170a016b12120a10ffeeddccbbaa99887766554433221100"

        assertEquals(hex, encodeHex(Holder.serializer(), value))
        assertEquals(value, decodeHex(Holder.serializer(), hex))
    }

    @Test
    fun `nullable field round trips null and a value`() {
        val withValue = "0a120a1000112233445566778899aabbccddeeff" +
                "22120a10ffeeddccbbaa99887766554433221100"

        assertEquals("0a120a1000112233445566778899aabbccddeeff", encodeHex(Holder.serializer(), Holder(a)))
        assertEquals(Holder(a), decodeHex(Holder.serializer(), "0a120a1000112233445566778899aabbccddeeff"))
        assertEquals(withValue, encodeHex(Holder.serializer(), Holder(a, nullable = b)))
        assertEquals(Holder(a, nullable = b), decodeHex(Holder.serializer(), withValue))
    }

    @Test
    fun `absent value is a missing field`() {
        assertMissingField("surf.types.v1.Uuid", "value") { decodeHex(UuidSerializer, "") }
        assertMissingField("surf.types.v1.Uuid", "value") { decodeHex(Holder.serializer(), "0a00") }
    }

    @Test
    fun `value of the wrong length is rejected`() {
        assertInvalid { decodeHex(UuidSerializer, "0a00") }
        assertInvalid { decodeHex(UuidSerializer, "0a0f00112233445566778899aabbccddee") }
        assertInvalid { decodeHex(UuidSerializer, "0a1100112233445566778899aabbccddeeff00") }
    }
}
