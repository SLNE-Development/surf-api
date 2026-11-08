package dev.slne.surf.api.core.serializer.java.datetime.zone.id

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneId

typealias SerializableZoneId = @Serializable(with = ZonedIdSerializer::class) ZoneId

object ZonedIdSerializer : KSerializer<ZoneId> {
    override val descriptor =
        PrimitiveSerialDescriptor("surf.api.java.datetime.zone.ZoneId", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: ZoneId,
    ) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder): ZoneId =
        ZoneId.of(decoder.decodeString())
}