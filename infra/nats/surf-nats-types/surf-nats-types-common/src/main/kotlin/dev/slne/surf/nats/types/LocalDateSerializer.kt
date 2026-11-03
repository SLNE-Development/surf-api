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
import java.time.LocalDate

object LocalDateSerializer : KSerializer<LocalDate> {
    override val descriptor: SerialDescriptor = LocalDateProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: LocalDate) {
        encoder.encodeSerializableValue(
            LocalDateProto.serializer(),
            LocalDateProto(value.year, value.monthValue, value.dayOfMonth)
        )
    }

    override fun deserialize(decoder: Decoder): LocalDate {
        val proto = decoder.decodeSerializableValue(LocalDateProto.serializer())

        return try {
            LocalDate.of(proto.year, proto.month, proto.day)
        } catch (e: DateTimeException) {
            throw SerializationException(
                "${descriptor.serialName} ${proto.year}-${proto.month}-${proto.day} is not a valid date",
                e
            )
        }
    }
}

@Serializable
@SerialName("surf.types.v1.LocalDate")
private class LocalDateProto(
    @ProtoNumber(1) val year: Int,
    @ProtoNumber(2) val month: Int,
    @ProtoNumber(3) val day: Int,
)
