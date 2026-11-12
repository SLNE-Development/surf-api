package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.protobuf.ProtoNumber
import net.kyori.adventure.key.InvalidKeyException
import net.kyori.adventure.key.Key

/**
 * Serializes an Adventure [Key] as the protobuf message `surf.types.v1.Key`.
 *
 * Field `value` (1, required) holds [Key.asString], `namespace:value`. A value without namespace decodes in the
 * `minecraft` namespace. The `dev.slne.surf.nats` Gradle plugin maps fields of this message type to [Key] with this
 * serializer.
 *
 * Decoding throws [kotlinx.serialization.MissingFieldException] when `value` is absent and
 * [kotlinx.serialization.SerializationException] when it is not a valid key.
 */
object KeySerializer : KSerializer<Key> {
    override val descriptor: SerialDescriptor = KeyProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Key) {
        encoder.encodeSerializableValue(KeyProto.serializer(), KeyProto(value.asString()))
    }

    override fun deserialize(decoder: Decoder): Key {
        val value = decoder.decodeSerializableValue(KeyProto.serializer()).value

        return try {
            Key.key(value)
        } catch (e: InvalidKeyException) {
            throw SerializationException("${descriptor.serialName}.value '$value' is not a valid key", e)
        }
    }
}

@Serializable
@SerialName("surf.types.v1.Key")
private class KeyProto(
    @ProtoNumber(1) val value: String,
)
