package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override val descriptor: SerialDescriptor = LocalDateTimeProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeSerializableValue(
            LocalDateTimeProto.serializer(),
            LocalDateTimeProto(value.toLocalDate(), value.toLocalTime())
        )
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        val proto = decoder.decodeSerializableValue(LocalDateTimeProto.serializer())

        return LocalDateTime.of(proto.date, proto.time)
    }
}

@Serializable
@SerialName("surf.types.v1.LocalDateTime")
private class LocalDateTimeProto(
    @ProtoNumber(1) val date: @Serializable(with = LocalDateSerializer::class) LocalDate,
    @ProtoNumber(2) val time: @Serializable(with = LocalTimeSerializer::class) LocalTime,
)
