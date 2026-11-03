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
