package dev.slne.surf.api.core.serializer.adventure.component.textcolor

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.format.TextColor

typealias SerializableTextColor = @Serializable(with = AdventureTextColorSerializer::class) TextColor

object AdventureTextColorSerializer : KSerializer<TextColor> {
    override val descriptor = PrimitiveSerialDescriptor("surf.api.TextColor", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: TextColor,
    ) {
        encoder.encodeInt(value.value())
    }

    override fun deserialize(decoder: Decoder): TextColor {
        return TextColor.color(decoder.decodeInt())
    }
}