package dev.slne.surf.surfapi.core.api.serializer.java.datetime.zone.offset

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.ZoneOffset

typealias SerializableZoneOffset = @Serializable(with = ZoneOffsetSerializer::class) ZoneOffset

object ZoneOffsetSerializer : KSerializer<ZoneOffset> {
    override val descriptor = PrimitiveSerialDescriptor(
        "surfapi.java.datetime.zone.ZoneOffset",
        PrimitiveKind.STRING
    )

    override fun serialize(
        encoder: Encoder,
        value: ZoneOffset,
    ) {
        encoder.encodeString(value.id)
    }

    override fun deserialize(decoder: Decoder) =
        ZoneOffset.of(decoder.decodeString())
}