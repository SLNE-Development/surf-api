package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.text.format.TextColor

object TextColorSerializer : KSerializer<TextColor> {
    private const val MAX_RGB = 0xFFFFFF

    override val descriptor: SerialDescriptor = TextColorProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: TextColor) {
        encoder.encodeSerializableValue(TextColorProto.serializer(), TextColorProto(value.value()))
    }

    override fun deserialize(decoder: Decoder): TextColor {
        val rgb = decoder.decodeSerializableValue(TextColorProto.serializer()).rgb
        if (rgb !in 0..MAX_RGB) {
            throw SerializationException("${descriptor.serialName}.rgb must be in 0..$MAX_RGB, was $rgb")
        }

        return TextColor.color(rgb)
    }
}

@Serializable
@SerialName("surf.types.v1.TextColor")
private class TextColorProto(
    @ProtoNumber(1) val rgb: Int,
)
