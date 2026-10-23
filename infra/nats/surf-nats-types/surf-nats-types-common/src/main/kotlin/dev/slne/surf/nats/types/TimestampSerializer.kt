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
