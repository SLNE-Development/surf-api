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

/**
 * Serializes a [LocalDate] as the protobuf message `surf.types.v1.LocalDate`.
 *
 * The fields `year` (1, proleptic ISO year, year 0 is 1 BC), `month` (2, 1 to 12) and `day` (3, day of the month) are
 * required. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [LocalDate] with this
 * serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when a field is absent and
 * [kotlinx.serialization.SerializationException] when the fields do not form a valid date.
 */
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
