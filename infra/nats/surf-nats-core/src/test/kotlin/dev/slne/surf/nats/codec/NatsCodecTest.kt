package dev.slne.surf.nats.codec

import dev.slne.surf.nats.WireFormat
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ByteArraySerializer
import kotlinx.serialization.modules.EmptySerializersModule
import java.nio.ByteBuffer
import java.util.*
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class NatsCodecTest {

    @Serializable
    data class Sample(
        val playerUuid: @Contextual UUID,
        val displayName: String,
        val level: Int = 1
    )

    private val codec = NatsCodec(EmptySerializersModule())
    private val sample = Sample(UUID.randomUUID(), "Twisti", level = 7)

    @Test
    fun `cbor roundtrip`() {
        val bytes = codec.encode(Sample.serializer(), sample, WireFormat.CBOR)
        assertEquals(sample, codec.decode(Sample.serializer(), bytes, WireFormat.CBOR.contentType))
    }

    @Test
    fun `json roundtrip`() {
        val bytes = codec.encode(Sample.serializer(), sample, WireFormat.JSON)
        assertEquals(sample, codec.decode(Sample.serializer(), bytes, WireFormat.JSON.contentType))
    }

    @Test
    fun `missing content type is rejected with the supported content types`() {
        val bytes = codec.encode(Sample.serializer(), sample, WireFormat.CBOR)
        val message = assertFailsWith<IllegalArgumentException> {
            codec.decode(Sample.serializer(), bytes, null)
        }.message.orEmpty()

        assertContains(message, "Missing Content-Type")
        WireFormat.entries.forEach { assertContains(message, it.contentType) }
    }

    @Test
    fun `content type parameters and case are ignored`() {
        val bytes = codec.encode(Sample.serializer(), sample, WireFormat.JSON)
        assertEquals(
            sample,
            codec.decode(Sample.serializer(), bytes, "Application/JSON; charset=utf-8")
        )
    }

    @Test
    fun `unknown content type is rejected with the supported content types`() {
        val bytes = codec.encode(Sample.serializer(), sample, WireFormat.JSON)
        val message = assertFailsWith<IllegalArgumentException> {
            codec.decode(Sample.serializer(), bytes, "text/plain")
        }.message.orEmpty()

        assertContains(message, "text/plain")
        WireFormat.entries.forEach { assertContains(message, it.contentType) }
    }

    @Test
    fun `json uses snake case and string uuids`() {
        val json = codec.encode(Sample.serializer(), sample, WireFormat.JSON).decodeToString()
        assertTrue("\"player_uuid\":\"${sample.playerUuid}\"" in json, json)
        assertTrue("\"display_name\":\"Twisti\"" in json, json)
    }

    @Test
    fun `json ignores unknown keys and fills defaults`() {
        val json = """{"player_uuid":"${sample.playerUuid}","display_name":"Twisti","unknown":true}"""
        val decoded =
            codec.decode(Sample.serializer(), json.encodeToByteArray(), WireFormat.JSON.contentType)
        assertEquals(Sample(sample.playerUuid, "Twisti"), decoded)
    }

    @Test
    fun `cbor does not encode uuid as string`() {
        val cbor = codec.encode(Sample.serializer(), sample, WireFormat.CBOR).asList()
        val uuidString = sample.playerUuid.toString().encodeToByteArray().asList()

        assertTrue(
            cbor.windowed(uuidString.size).none { it == uuidString },
            "CBOR payload contains the UUID as string"
        )
    }

    @Test
    fun `cbor encodes uuid as 16 byte string`() {
        val uuid = UUID.randomUUID()
        val bytes = codec.encode(Sample.serializer(), Sample(uuid, "x"), WireFormat.CBOR)

        val expected = byteArrayOf(0x50) + ByteBuffer.allocate(16)
            .putLong(uuid.mostSignificantBits)
            .putLong(uuid.leastSignificantBits)
            .array()
        assertTrue(
            bytes.asList().windowed(expected.size).any { it == expected.asList() },
            "CBOR payload does not contain the UUID as byte string (major type 2, length 16)"
        )
    }

    @Test
    fun `cbor encodes byte arrays as byte string`() {
        val bytes = codec.encode(ByteArraySerializer(), ByteArray(64) { it.toByte() }, WireFormat.CBOR)

        assertEquals(2 + 64, bytes.size, "64 bytes should be a byte string with a 2 byte header")
        assertEquals(0x58, bytes[0].toInt() and 0xFF)
    }
}
