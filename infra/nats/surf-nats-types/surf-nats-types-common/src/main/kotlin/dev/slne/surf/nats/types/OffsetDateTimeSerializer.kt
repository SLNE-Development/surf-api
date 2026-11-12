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
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * Serializes an [OffsetDateTime] as the protobuf message `surf.types.v1.OffsetDateTime`.
 *
 * The message has the following fields:
 * - `seconds` (1, required): seconds of the instant since 1970-01-01T00:00:00Z.
 * - `nanos` (2, optional, default 0): nanosecond of the second, 0 to 999,999,999.
 * - `offset_seconds` (3, required): the UTC offset in seconds, -64,800 to 64,800.
 *
 * Instant and offset are preserved exactly, so the decoded value equals the original. The `dev.slne.surf.nats` Gradle
 * plugin maps fields of this message type to [OffsetDateTime] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `seconds` or `offset_seconds` is absent and
 * [kotlinx.serialization.SerializationException] when `nanos` or the offset is out of range or the instant lies
 * outside the range of [OffsetDateTime].
 */
object OffsetDateTimeSerializer : KSerializer<OffsetDateTime> {
    private const val MAX_NANOS = 999_999_999

    override val descriptor: SerialDescriptor = OffsetDateTimeProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: OffsetDateTime) {
        encoder.encodeSerializableValue(
            OffsetDateTimeProto.serializer(),
            OffsetDateTimeProto(value.toEpochSecond(), value.nano, value.offset.totalSeconds)
        )
    }

    override fun deserialize(decoder: Decoder): OffsetDateTime {
        val proto = decoder.decodeSerializableValue(OffsetDateTimeProto.serializer())
        if (proto.nanos !in 0..MAX_NANOS) {
            throw SerializationException("${descriptor.serialName}.nanos must be in 0..$MAX_NANOS, was ${proto.nanos}")
        }

        val offset = try {
            ZoneOffset.ofTotalSeconds(proto.offsetSeconds)
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName}.offset_seconds ${proto.offsetSeconds} is not a valid offset",
                e
            )
        }

        return try {
            OffsetDateTime.ofInstant(Instant.ofEpochSecond(proto.seconds, proto.nanos.toLong()), offset)
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName}.seconds ${proto.seconds} is outside the java.time.OffsetDateTime range",
                e
            )
        }
    }
}

@Serializable
@SerialName("surf.types.v1.OffsetDateTime")
private class OffsetDateTimeProto(
    @ProtoNumber(1) val seconds: Long,
    @ProtoNumber(2) val nanos: Int = 0,
    @ProtoNumber(3) val offsetSeconds: Int,
)
