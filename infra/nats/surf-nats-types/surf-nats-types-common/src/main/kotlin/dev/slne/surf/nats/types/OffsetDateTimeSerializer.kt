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
