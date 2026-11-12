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
import java.time.LocalTime

/**
 * Serializes a [LocalTime] as the protobuf message `surf.types.v1.LocalTime`.
 *
 * The optional fields `hour` (1, 0 to 23), `minute` (2, 0 to 59), `second` (3, 0 to 59) and `nanos` (4, 0 to
 * 999,999,999) default to 0, so an empty message is midnight. Nanosecond precision is preserved. The
 * `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [LocalTime] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.SerializationException] when a field is out of range.
 */
object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor: SerialDescriptor = LocalTimeProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: LocalTime) {
        encoder.encodeSerializableValue(
            LocalTimeProto.serializer(),
            LocalTimeProto(value.hour, value.minute, value.second, value.nano)
        )
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        val proto = decoder.decodeSerializableValue(LocalTimeProto.serializer())

        return try {
            LocalTime.of(proto.hour, proto.minute, proto.second, proto.nanos)
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName} ${proto.hour}:${proto.minute}:${proto.second}.${proto.nanos} is not a valid time",
                e
            )
        }
    }
}

@Serializable
@SerialName("surf.types.v1.LocalTime")
private class LocalTimeProto(
    @ProtoNumber(1) val hour: Int = 0,
    @ProtoNumber(2) val minute: Int = 0,
    @ProtoNumber(3) val second: Int = 0,
    @ProtoNumber(4) val nanos: Int = 0,
)
