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
