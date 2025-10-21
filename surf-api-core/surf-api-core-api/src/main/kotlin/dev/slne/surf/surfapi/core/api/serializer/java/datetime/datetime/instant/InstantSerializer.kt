package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime.instant

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

typealias SerializableInstant = @Serializable(with = InstantSerializer::class) Instant

object InstantSerializer : KSerializer<Instant> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surfapi.java.datetime.datetime.Instant",
        PrimitiveKind.LONG
    )

    override fun serialize(
        encoder: Encoder,
        value: Instant,
    ) {
        encoder.encodeLong(value.toEpochMilli())
    }

    override fun deserialize(decoder: Decoder): Instant =
        Instant.ofEpochMilli(decoder.decodeLong())
}