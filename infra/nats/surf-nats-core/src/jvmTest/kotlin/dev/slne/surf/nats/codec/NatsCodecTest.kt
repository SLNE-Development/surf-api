@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.nats.codec

import dev.slne.surf.nats.TEST_UUID
import dev.slne.surf.nats.everything
import dev.slne.surf.nats.protocol.ProtocolHeaders
import dev.slne.surf.nats.test.proto.v1.Color
import dev.slne.surf.nats.test.proto.v1.ColorHolder
import dev.slne.surf.nats.test.proto.v1.Created
import dev.slne.surf.nats.test.proto.v1.Everything
import dev.slne.surf.nats.test.proto.v1.FindRequest
import dev.slne.surf.nats.test.proto.v1.Heartbeat
import dev.slne.surf.nats.test.proto.v1.PlayerEvent
import dev.slne.surf.nats.test.proto.v1.ProfileUpdated
import dev.slne.surf.nats.test.proto.v1.Text
import dev.slne.surf.nats.test.proto.v1.WideColor
import dev.slne.surf.nats.test.proto.v1.WideColorHolder
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.MissingFieldException
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import dev.slne.surf.nats.test.proto.v1.VersionedChanged as VersionedChangedV1
import dev.slne.surf.nats.test.proto.v2.VersionedChanged as VersionedChangedV2

class NatsCodecTest {

    private fun <T> roundtrip(serializer: KSerializer<T>, value: T): T {
        return NatsCodec.decode(serializer, NatsCodec.encode(serializer, value), ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
    }

    @Test
    fun `every supported field kind roundtrips`() {
        assertEquals(everything(), roundtrip(Everything.serializer(), everything()))
    }

    @Test
    fun `missing content type is rejected naming the supported type`() {
        val bytes = NatsCodec.encode(Created.serializer(), Created(1))

        val message = assertFailsWith<IllegalArgumentException> {
            NatsCodec.decode(Created.serializer(), bytes, null)
        }.message.orEmpty()

        assertContains(message, "Missing Content-Type")
        assertContains(message, "application/protobuf")
    }

    @Test
    fun `content type parameters and case are ignored`() {
        val bytes = NatsCodec.encode(Created.serializer(), Created(1))

        assertEquals(Created(1), NatsCodec.decode(Created.serializer(), bytes, "Application/Protobuf; charset=binary"))
    }

    @Test
    fun `other content types are rejected naming the supported type`() {
        val bytes = NatsCodec.encode(Created.serializer(), Created(1))

        for (contentType in listOf("application/cbor", "application/json", "")) {
            val message = assertFailsWith<IllegalArgumentException>("content type '$contentType'") {
                NatsCodec.decode(Created.serializer(), bytes, contentType)
            }.message.orEmpty()
            assertContains(message, "Unsupported content type '$contentType'")
            assertContains(message, "application/protobuf")
        }
    }

    @Test
    fun `unknown fields are skipped`() {
        val newer = NatsCodec.encode(VersionedChangedV2.serializer(), VersionedChangedV2("hello", revision = 3))

        val decoded = NatsCodec.decode(VersionedChangedV1.serializer(), newer, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

        assertEquals(VersionedChangedV1("hello"), decoded)
    }

    @Test
    fun `missing non-null field is a decode error`() {
        assertFailsWith<MissingFieldException> {
            NatsCodec.decode(Created.serializer(), ByteArray(0), ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
        }
    }

    @Test
    fun `truncated payload is a serialization error`() {
        // Field 1, length-delimited, announces 5 bytes that do not follow
        val truncated = byteArrayOf(0x0a, 0x05)

        for (serializer in listOf(Text.serializer(), FindRequest.serializer())) {
            val exception = assertFailsWith<SerializationException>(serializer.descriptor.serialName) {
                NatsCodec.decode(serializer, truncated, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)
            }
            assertContains(exception.message.orEmpty(), serializer.descriptor.serialName)
        }
    }

    @Test
    fun `unknown enum numbers decode to the zero entry in every position`() {
        val wide = WideColorHolder(
            color = WideColor.PURPLE,
            maybeColor = WideColor.PURPLE,
            colors = listOf(WideColor.PURPLE, WideColor.RED),
            colorsByName = mapOf("unknown" to WideColor.PURPLE, "known" to WideColor.BLUE),
        )
        val bytes = NatsCodec.encode(WideColorHolder.serializer(), wide)

        val decoded = NatsCodec.decode(ColorHolder.serializer(), bytes, ProtocolHeaders.PROTOBUF_CONTENT_TYPE)

        val expected = ColorHolder(
            color = Color.UNSPECIFIED,
            maybeColor = Color.UNSPECIFIED,
            colors = listOf(Color.UNSPECIFIED, Color.RED),
            colorsByName = mapOf("unknown" to Color.UNSPECIFIED, "known" to Color.BLUE),
        )
        assertEquals(expected, decoded)
    }

    @Test
    fun `null fields are not encoded and decode as null`() {
        val withoutOptionals = ProfileUpdated(TEST_UUID)

        val bytes = NatsCodec.encode(ProfileUpdated.serializer(), withoutOptionals)

        assertContentEquals(NatsCodec.encode(PlayerEvent.serializer(), PlayerEvent(TEST_UUID)), bytes)
        assertEquals(withoutOptionals, roundtrip(ProfileUpdated.serializer(), withoutOptionals))
    }

    @Test
    fun `empty values of nullable fields stay distinct from null`() {
        val empty = ProfileUpdated(TEST_UUID, nickname = "")

        assertEquals(empty, roundtrip(ProfileUpdated.serializer(), empty))
    }

    @Test
    fun `uuid is encoded as a 16 byte big-endian message`() {
        val bytes = NatsCodec.encode(PlayerEvent.serializer(), PlayerEvent(TEST_UUID))

        assertEquals("0a120a10123e4567e89b12d3a456426614174000", bytes.joinToString("") { "%02x".format(it) })
    }

    @Test
    fun `values equal to their default are not encoded`() {
        val withDefault = NatsCodec.encode(Everything.serializer(), everything().copy(defaulted = 7))
        val withOther = NatsCodec.encode(Everything.serializer(), everything().copy(defaulted = 8))

        // field 27 as varint: two tag bytes and one value byte
        assertEquals(withDefault.size + 3, withOther.size)
        assertEquals(0, NatsCodec.encode(Heartbeat.serializer(), Heartbeat()).size)
        assertEquals(Heartbeat(), roundtrip(Heartbeat.serializer(), Heartbeat()))
    }
}
