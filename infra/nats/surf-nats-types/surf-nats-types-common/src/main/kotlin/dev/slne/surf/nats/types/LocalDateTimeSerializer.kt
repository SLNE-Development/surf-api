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

/**
 * Serializes a [LocalDateTime] as the protobuf message `surf.types.v1.LocalDateTime`.
 *
 * The fields `date` (1, `surf.types.v1.LocalDate`, see [LocalDateSerializer]) and `time` (2,
 * `surf.types.v1.LocalTime`, see [LocalTimeSerializer]) are required. No offset or zone is written. The
 * `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [LocalDateTime] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `date` or `time` is absent and
 * [kotlinx.serialization.SerializationException] when either is invalid.
 */
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
