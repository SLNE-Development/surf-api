package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.DateTimeException
import java.time.Instant

/**
 * Serializes an [Instant] as the well-known protobuf message `google.protobuf.Timestamp`.
 *
 * Field `seconds` (1) holds the seconds since 1970-01-01T00:00:00Z and `nanos` (2) the nanosecond of the second, 0 to
 * 999,999,999, also for instants before the epoch. Both default to 0, so an empty message is the epoch. Nanosecond
 * precision is preserved. Encoding does not restrict seconds to the years 1 to 9999 that the protobuf specification
 * names. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [Instant] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.SerializationException] when `nanos` is out of range or `seconds` lies
 * outside the range of [Instant].
 */
object TimestampSerializer : KSerializer<Instant> {
    private const val MAX_NANOS = 999_999_999

    override val descriptor: SerialDescriptor = TimestampProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeSerializableValue(
            TimestampProto.serializer(),
            TimestampProto(value.epochSecond, value.nano)
        )
    }

    override fun deserialize(decoder: Decoder): Instant {
        val proto = decoder.decodeSerializableValue(TimestampProto.serializer())
        if (proto.nanos !in 0..MAX_NANOS) {
            throw SerializationException("${descriptor.serialName}.nanos must be in 0..$MAX_NANOS, was ${proto.nanos}")
        }

        return try {
            Instant.ofEpochSecond(proto.seconds, proto.nanos.toLong())
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName}.seconds ${proto.seconds} is outside the java.time.Instant range",
                e
            )
        }
    }
}

@Serializable
@SerialName("google.protobuf.Timestamp")
private class TimestampProto(
    @ProtoNumber(1) val seconds: Long = 0,
    @ProtoNumber(2) val nanos: Int = 0,
)
