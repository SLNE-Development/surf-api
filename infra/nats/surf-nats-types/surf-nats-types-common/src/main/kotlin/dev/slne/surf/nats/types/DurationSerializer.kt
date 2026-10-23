package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds
import kotlin.time.Duration.Companion.seconds

object DurationSerializer : KSerializer<Duration> {
    private const val MAX_SECONDS = 315_576_000_000L
    private const val MAX_NANOS = 999_999_999

    override val descriptor: SerialDescriptor = DurationProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Duration) {
        if (value.isInfinite()) {
            throw SerializationException("${descriptor.serialName} cannot represent the infinite duration $value")
        }

        val proto = value.toComponents { seconds, nanoseconds ->
            checkSeconds(seconds)
            DurationProto(seconds, nanoseconds)
        }

        encoder.encodeSerializableValue(DurationProto.serializer(), proto)
    }

    override fun deserialize(decoder: Decoder): Duration {
        val proto = decoder.decodeSerializableValue(DurationProto.serializer())
        val seconds = proto.seconds
        val nanos = proto.nanos

        checkSeconds(seconds)
        if (nanos !in -MAX_NANOS..MAX_NANOS) {
            throw SerializationException("${descriptor.serialName}.nanos must be in -$MAX_NANOS..$MAX_NANOS, was $nanos")
        }
        if ((seconds > 0 && nanos < 0) || (seconds < 0 && nanos > 0)) {
            throw SerializationException(
                "${descriptor.serialName}.seconds ($seconds) and nanos ($nanos) must have the same sign"
            )
        }

        return seconds.seconds + nanos.nanoseconds
    }

    private fun checkSeconds(seconds: Long) {
        if (seconds !in -MAX_SECONDS..MAX_SECONDS) {
            throw SerializationException(
                "${descriptor.serialName}.seconds must be in -$MAX_SECONDS..$MAX_SECONDS, was $seconds"
            )
        }
    }
}

@Serializable
@SerialName("google.protobuf.Duration")
private class DurationProto(
    @ProtoNumber(1) val seconds: Long = 0,
    @ProtoNumber(2) val nanos: Int = 0,
)
