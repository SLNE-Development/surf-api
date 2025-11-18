package dev.slne.surf.surfapi.core.api.serializer.java.datetime.time.local

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.LocalTime

typealias SerializableLocalTime = @Serializable(with = LocalTimeSerializer::class) LocalTime

object LocalTimeSerializer : KSerializer<LocalTime> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surfapi.java.datetime.time.LocalTime",
        PrimitiveKind.LONG
    )

    override fun serialize(
        encoder: Encoder,
        value: LocalTime,
    ) {
        encoder.encodeLong(value.toNanoOfDay())
    }

    override fun deserialize(decoder: Decoder): LocalTime {
        return LocalTime.ofNanoOfDay(decoder.decodeLong())
    }
}