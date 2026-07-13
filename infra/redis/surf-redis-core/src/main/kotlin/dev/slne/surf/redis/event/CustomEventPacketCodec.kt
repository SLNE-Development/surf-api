package dev.slne.surf.redis.event

import dev.slne.surf.redis.codec.*
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.redisson.client.codec.BaseCodec
import org.redisson.client.protocol.Decoder
import org.redisson.client.protocol.Encoder

internal object CustomEventPacketCodec {
    const val CHANNEL = "surf-redis:events:binary"
    const val MAX_PACKET_SIZE = 16 * 1024 * 1024 // 16 MiB
    const val DEFAULT_INITIAL_CAPACITY = 512

    private const val MAGIC = 0x53524531
    private const val PROTOCOL_VERSION = 1
    private const val MAX_ORIGIN_ID_BYTES = 256

    class OutboundPacket internal constructor(
        internal val event: RedisEvent,
        internal val registration: EventCodecRegistration
    )

    fun outbound(event: RedisEvent, registration: EventCodecRegistration) =
        OutboundPacket(event, registration)

    fun redisCodec(
        resolver: (String) -> EventCodecRegistration?
    ) = object : BaseCodec() {
        private val encoder = Encoder { value ->
            val packet = value as? OutboundPacket
                ?: throw RedisCodecException(
                    "Binary Redis event codec expected an outbound event packet but received '${value?.javaClass?.name}'"
                )
            encode(packet.event, packet.registration)
        }
        private val decoder = Decoder<Any> { buffer, _ ->
            try {
                decode(buffer, resolver)
            } catch (failure: RedisCodecException) {
                DecodeResult.Failure(failure)
            }
        }

        override fun getValueEncoder(): Encoder = encoder
        override fun getValueDecoder(): Decoder<Any> = decoder
    }

    private fun encode(event: RedisEvent, registration: EventCodecRegistration): ByteBuf {
        val initialCapacity = registration.packetSizeEstimate.coerceIn(1, MAX_PACKET_SIZE)
        val buffer = ByteBufAllocator.DEFAULT.buffer(initialCapacity, MAX_PACKET_SIZE)
        try {
            buffer.writeInt(MAGIC)
            buffer.writeByte(PROTOCOL_VERSION)
            buffer.writeString(registration.eventId, EventCodecRegistry.MAX_EVENT_ID_BYTES)
            buffer.writeVarInt(registration.version)
            buffer.writeLong(event.timestamp)
            buffer.writeNullable(event.originId) { target, origin ->
                target.writeString(origin, MAX_ORIGIN_ID_BYTES)
            }

            try {
                registration.codec.encode(buffer, event)
            } catch (e: Exception) {
                throw RedisCodecException(
                    "Codec '${registration.codec.codecId}' failed to encode event '${registration.eventType.name}'",
                    e
                )
            }

            val size = buffer.readableBytes()
            if (size > MAX_PACKET_SIZE) {
                throw RedisCodecException(
                    "Event codec '${registration.codec.codecId}' produced a $size-byte packet for '${registration.eventType.name}'; maximum is $MAX_PACKET_SIZE"
                )
            }
            registration.recordPacketSize(size)
            return buffer
        } catch (failure: Throwable) {
            buffer.release()
            throw failure
        }
    }

    private fun decode(
        buffer: ByteBuf,
        resolver: (String) -> EventCodecRegistration?
    ): DecodeResult {
        val packetSize = buffer.readableBytes()
        if (packetSize > MAX_PACKET_SIZE) {
            throw RedisCodecException(
                "Custom Redis event packet is $packetSize bytes; maximum is $MAX_PACKET_SIZE"
            )
        }

        try {
            if (buffer.readInt() != MAGIC) {
                throw RedisCodecException("Custom Redis event packet has invalid magic")
            }
            val protocolVersion = buffer.readUnsignedByte().toInt()
            if (protocolVersion != PROTOCOL_VERSION) {
                throw RedisCodecException(
                    "Unsupported custom Redis event protocol version: $protocolVersion"
                )
            }

            val eventId = buffer.readString(EventCodecRegistry.MAX_EVENT_ID_BYTES)
            val codecVersion = buffer.readVarInt()
            val timestamp = buffer.readLong()
            val originId = buffer.readNullable { it.readString(MAX_ORIGIN_ID_BYTES) }
            val registration = resolver(eventId)
                ?: return DecodeResult.MissingCodec(eventId, codecVersion)

            if (registration.version != codecVersion) {
                return DecodeResult.VersionMismatch(registration, codecVersion)
            }

            val event = try {
                registration.codec.decode(buffer)
            } catch (e: Exception) {
                throw RedisCodecException(
                    "Codec '${registration.codec.codecId}' failed to decode event '${registration.eventType.name}'",
                    e
                )
            }
            if (!registration.eventType.isInstance(event)) {
                throw RedisCodecException(
                    "Codec '${registration.codec.codecId}' decoded '${event.javaClass.name}' for event '${registration.eventType.name}'"
                )
            }
            if (buffer.isReadable) {
                throw RedisCodecException(
                    "Codec '${registration.codec.codecId}' left ${buffer.readableBytes()} unread bytes for event '${registration.eventType.name}'"
                )
            }
            return DecodeResult.Event(registration, event, timestamp, originId)
        } catch (e: RedisCodecException) {
            throw e
        } catch (e: Exception) {
            throw RedisCodecException("Malformed custom Redis event packet", e)
        }
    }

    sealed interface DecodeResult {
        data class Event(
            val registration: EventCodecRegistration,
            val event: RedisEvent,
            val timestamp: Long,
            val originId: String?
        ) : DecodeResult

        data class MissingCodec(val eventId: String, val codecVersion: Int) : DecodeResult

        data class VersionMismatch(
            val registration: EventCodecRegistration,
            val receivedVersion: Int
        ) : DecodeResult

        data class Failure(val exception: RedisCodecException) : DecodeResult
    }
}
