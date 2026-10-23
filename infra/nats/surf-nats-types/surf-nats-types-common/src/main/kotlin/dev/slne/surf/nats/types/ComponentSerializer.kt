package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer

object ComponentSerializer : KSerializer<Component> {
    override val descriptor: SerialDescriptor = ComponentProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Component) {
        encoder.encodeSerializableValue(
            ComponentProto.serializer(),
            ComponentProto(GsonComponentSerializer.gson().serialize(value.compact()))
        )
    }

    override fun deserialize(decoder: Decoder): Component {
        val json = decoder.decodeSerializableValue(ComponentProto.serializer()).json

        return try {
            GsonComponentSerializer.gson().deserialize(json)
        } catch (e: RuntimeException) {
            throw SerializationException("${descriptor.serialName}.json is not a valid component", e)
        }
    }
}

@Serializable
@SerialName("surf.types.v1.Component")
private class ComponentProto(
    @ProtoNumber(1) val json: String,
)
