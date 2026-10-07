package dev.slne.surf.nats.rpc

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object UpperCaseSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("test.UpperCase", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value.uppercase())
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}

class ReversedSerializer : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("test.Reversed", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value.reversed())
    override fun deserialize(decoder: Decoder): String = decoder.decodeString().reversed()
}
