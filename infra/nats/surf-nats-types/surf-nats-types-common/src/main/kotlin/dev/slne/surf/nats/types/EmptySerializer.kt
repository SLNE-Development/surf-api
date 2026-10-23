package dev.slne.surf.nats.types

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

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
