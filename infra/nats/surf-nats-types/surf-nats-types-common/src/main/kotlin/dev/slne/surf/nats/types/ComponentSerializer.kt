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

/**
 * Serializes an Adventure [Component] as the protobuf message `surf.types.v1.Component`.
 *
 * Field `json` (1, required) holds the component in the JSON format of [GsonComponentSerializer.gson]. The component
 * is compacted with [Component.compact] before encoding, so the decoded component renders the same but can differ
 * structurally from the original. For example, an empty text component whose only child is `Component.text("a")`
 * decodes as `Component.text("a")`. The `dev.slne.surf.nats` Gradle plugin maps fields
 * of this message type to [Component] with this serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `json` is absent and
 * [kotlinx.serialization.SerializationException] when it is not a valid component.
 */
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
