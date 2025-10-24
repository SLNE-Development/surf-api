package dev.slne.surf.surfapi.core.api.serializer.adventure.component.shadowcolor

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.format.ShadowColor

typealias SerializableShadowColor = @Serializable(with = AdventureShadowColorSerializer::class) ShadowColor

object AdventureShadowColorSerializer : KSerializer<ShadowColor> {
    override val descriptor = PrimitiveSerialDescriptor("surfapi.ShadowColor", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: ShadowColor,
    ) {
        encoder.encodeInt(value.value())
    }

    override fun deserialize(decoder: Decoder): ShadowColor {
        return ShadowColor.shadowColor(decoder.decodeInt())
    }
}