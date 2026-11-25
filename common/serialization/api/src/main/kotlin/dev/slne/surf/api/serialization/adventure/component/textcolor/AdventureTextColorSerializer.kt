package dev.slne.surf.api.serialization.adventure.component.textcolor

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import net.kyori.adventure.text.format.TextColor

typealias SerializableTextColor = @Serializable(
    with = AdventureTextColorSerializer::class,
) TextColor

/**
 * Serializes a [TextColor] as the integer `0xRRGGBB`.
 *
 * A value that matches a named color decodes as the
 * [net.kyori.adventure.text.format.NamedTextColor] constant. Decoding throws
 * [SerializationException] for a value outside `0` to `0xFFFFFF`.
 */
object AdventureTextColorSerializer : KSerializer<TextColor> {
    private const val MAX_RGB = 0xFFFFFF

    override val descriptor = PrimitiveSerialDescriptor("surf.api.TextColor", PrimitiveKind.INT)

    override fun serialize(
        encoder: Encoder,
        value: TextColor,
    ) {
        encoder.encodeInt(value.value())
    }

    override fun deserialize(decoder: Decoder): TextColor {
        val rgb = decoder.decodeInt()

        if (rgb !in 0..MAX_RGB) {
            throw SerializationException("A text color must be in 0..$MAX_RGB, was $rgb")
        }

        return TextColor.color(rgb)
    }
}
