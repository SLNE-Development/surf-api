package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Serializes [Unit] as the well-known protobuf message `google.protobuf.Empty`.
 *
 * Encoding writes no fields. Decoding skips any fields in the message. The `dev.slne.surf.nats` Gradle plugin maps
 * `google.protobuf.Empty`, for example as the return type of a service method, to [Unit] with this serializer.
 */
object EmptySerializer : KSerializer<Unit> {
    private val empty = EmptyProto()

    override val descriptor: SerialDescriptor = EmptyProto.serializer().descriptor

    override fun serialize(encoder: Encoder, value: Unit) {
        encoder.encodeSerializableValue(EmptyProto.serializer(), empty)
    }

    override fun deserialize(decoder: Decoder) {
        val _ = decoder.decodeSerializableValue(EmptyProto.serializer())
    }
}

@Serializable
@SerialName("google.protobuf.Empty")
private class EmptyProto
