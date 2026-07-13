package dev.slne.surf.redis.event

import dev.slne.surf.redis.codec.RedisCodecException
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import org.redisson.client.codec.Codec
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

class CustomEventPacketCodecTest {
    @Test
    fun `custom event packet round trips on the isolated binary protocol`() {
        val registry = registry(PacketCodec(version = 4))
        val registration = registry.codecForEventId(PacketCodec.EVENT_ID)!!
        val redisCodec = redisCodec(registry)
        val original = PacketEvent(91)

        val packet = encode(redisCodec, original, registration)
        val firstByte = packet.getByte(packet.readerIndex())
        val result = try {
            assertIs<CustomEventPacketCodec.DecodeResult.Event>(decode(redisCodec, packet))
        } finally {
            packet.release()
        }

        assertEquals(91, (result.event as PacketEvent).value)
        assertEquals(original.timestamp, result.timestamp)
        assertEquals(4, result.registration.version)
        assertEquals("surf-redis:events:binary", CustomEventPacketCodec.CHANNEL)
        assertNotEquals('{'.code.toByte(), firstByte)
    }

    @Test
    fun `missing codec is isolated after routing metadata is decoded`() {
        val registry = registry(PacketCodec())
        val registration = registry.codecForEventId(PacketCodec.EVENT_ID)!!
        val redisCodec = CustomEventPacketCodec.redisCodec { null }
        val packet = encode(redisCodec, PacketEvent(7), registration)

        val result = try {
            assertIs<CustomEventPacketCodec.DecodeResult.MissingCodec>(decode(redisCodec, packet))
        } finally {
            packet.release()
        }

        assertEquals(PacketCodec.EVENT_ID, result.eventId)
        assertEquals(1, result.codecVersion)
    }

    @Test
    fun `codec version mismatch does not invoke the decoder`() {
        val sending = registry(PacketCodec(version = 1))
        val receiving = registry(PacketCodec(version = 2))
        val redisCodec = redisCodec(receiving)
        val packet = encode(
            redisCodec,
            PacketEvent(8),
            sending.codecForEventId(PacketCodec.EVENT_ID)!!
        )

        val result = try {
            assertIs<CustomEventPacketCodec.DecodeResult.VersionMismatch>(
                decode(redisCodec, packet)
            )
        } finally {
            packet.release()
        }

        assertEquals(1, result.receivedVersion)
        assertEquals(2, result.registration.version)
    }

    @Test
    fun `serialization failure includes codec and event context`() {
        val codec = object : RedisEventCodec<PacketEvent> {
            override val eventId = "test:failing-event"
            override val codecId = "test-failing-codec"
            override fun encode(buffer: ByteBuf, value: PacketEvent) = error("boom")
            override fun decode(buffer: ByteBuf) = PacketEvent(0)
        }
        val registry = EventCodecRegistry().apply {
            registerExplicit(PacketEvent::class.java, codec)
            freeze()
        }

        val failure = assertFailsWith<RedisCodecException> {
            redisCodec(registry).valueEncoder.encode(
                CustomEventPacketCodec.outbound(
                    PacketEvent(1),
                    registry.codecForEventId(codec.eventId)!!
                )
            )
        }

        assertTrue(failure.message.orEmpty().contains("test-failing-codec"))
        assertTrue(failure.message.orEmpty().contains(PacketEvent::class.java.name))
    }

    @Test
    fun `redisson decoder reports malformed buffers as failure results`() {
        val redisCodec = CustomEventPacketCodec.redisCodec { null }
        val malformed = Unpooled.wrappedBuffer(byteArrayOf(1, 2, 3))

        val result = try {
            assertIs<CustomEventPacketCodec.DecodeResult.Failure>(
                decode(redisCodec, malformed)
            )
        } finally {
            malformed.release()
        }

        assertTrue(result.exception.message.orEmpty().contains("Malformed"))
    }

    @Test
    fun `moving packet size estimate adapts without sharing redisson buffers`() {
        val registry = registry(PacketCodec())
        val registration = registry.codecForEventId(PacketCodec.EVENT_ID)!!
        val redisCodec = redisCodec(registry)

        val first = encode(redisCodec, PacketEvent(42, ByteArray(1_024)), registration)
        val firstSize = first.readableBytes()
        assertEquals(firstSize, registration.packetSizeEstimate)

        val second = encode(redisCodec, PacketEvent(42, ByteArray(4_096)), registration)
        val secondSize = second.readableBytes()
        assertNotSame(first, second)
        assertEquals(firstSize + (secondSize - firstSize) / 4, registration.packetSizeEstimate)

        first.release()
        val result = try {
            assertIs<CustomEventPacketCodec.DecodeResult.Event>(decode(redisCodec, second))
        } finally {
            second.release()
        }
        val event = result.event as PacketEvent
        assertEquals(42, event.value)
        assertEquals(4_096, event.payload.size)
    }

    private fun redisCodec(registry: EventCodecRegistry): Codec =
        CustomEventPacketCodec.redisCodec(registry::codecForEventId)

    private fun encode(
        codec: Codec,
        event: PacketEvent,
        registration: EventCodecRegistration
    ): ByteBuf = codec.valueEncoder.encode(CustomEventPacketCodec.outbound(event, registration))

    private fun decode(codec: Codec, packet: ByteBuf): CustomEventPacketCodec.DecodeResult =
        codec.valueDecoder.decode(packet, null) as CustomEventPacketCodec.DecodeResult

    private fun registry(codec: PacketCodec): EventCodecRegistry = EventCodecRegistry().apply {
        registerExplicit(PacketEvent::class.java, codec)
        freeze()
    }

    private class PacketEvent(
        val value: Int,
        val payload: ByteArray = byteArrayOf()
    ) : RedisEvent()

    private class PacketCodec(
        override val version: Int = 1
    ) : RedisEventCodec<PacketEvent> {
        override val eventId = EVENT_ID
        override val codecId = "test-packet-codec"

        override fun encode(buffer: ByteBuf, value: PacketEvent) {
            buffer.writeInt(value.value)
            buffer.writeBytes(value.payload)
        }

        override fun decode(buffer: ByteBuf): PacketEvent {
            val value = buffer.readInt()
            val payload = ByteArray(buffer.readableBytes())
            buffer.readBytes(payload)
            return PacketEvent(value, payload)
        }

        companion object {
            const val EVENT_ID = "test:packet-event"
        }
    }
}
