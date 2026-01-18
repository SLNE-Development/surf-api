package dev.slne.surf.surfapi.core.api.serializer.java.datetime.datetime

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

abstract class UtcInstantDateTimeSerializer<T : Any>(
    serialName: String
) : KSerializer<T> {
    override val descriptor = PrimitiveSerialDescriptor(serialName, PrimitiveKind.LONG)

    override fun serialize(encoder: Encoder, value: T) {
        val instant = toInstant(value)

        encoder.encodeLong(instant.epochSecond)
    }

    override fun deserialize(decoder: Decoder): T {
        val instant = Instant.ofEpochSecond(decoder.decodeLong())

        return fromInstant(instant)
    }

    protected abstract fun toInstant(value: T): Instant
    protected abstract fun fromInstant(instant: Instant): T
}