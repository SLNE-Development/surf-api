package dev.slne.surf.surfapi.core.api.serializer.java.datetime.date.date

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.util.*

typealias SerializableDate = @Serializable(with = DateSerializer::class) Date

object DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surfapi.java.datetime.date.Date",
        PrimitiveKind.LONG
    )

    override fun serialize(encoder: Encoder, value: Date) {
        encoder.encodeLong(value.time)
    }

    override fun deserialize(decoder: Decoder) = Date(decoder.decodeLong())
}