package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoIntegerType
import kotlinx.serialization.protobuf.ProtoNumber
import kotlinx.serialization.protobuf.ProtoType
import net.kyori.adventure.text.format.ShadowColor

/**
 * Serializes an Adventure [ShadowColor] as the protobuf message `surf.types.v1.ShadowColor`.
 *
 * Field `argb` (1, required, `fixed32`) holds the color as `0xAARRGGBB`. Every 32-bit value is a valid color,
 * including fully transparent `0`. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to
 * [ShadowColor] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `argb` is absent.
 */
object ShadowColorSerializer : KSerializer<ShadowColor> {
    override val descriptor: SerialDescriptor = ShadowColorProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ShadowColor) {
        encoder.encodeSerializableValue(ShadowColorProto.serializer(), ShadowColorProto(value.value()))
    }

    override fun deserialize(decoder: Decoder): ShadowColor {
        return ShadowColor.shadowColor(decoder.decodeSerializableValue(ShadowColorProto.serializer()).argb)
    }
}

@Serializable
@SerialName("surf.types.v1.ShadowColor")
private class ShadowColorProto(
    @ProtoNumber(1) @ProtoType(ProtoIntegerType.FIXED) val argb: Int,
)
