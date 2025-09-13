package dev.slne.surf.surfapi.core.api.kotlinx.serializer.java

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI

typealias SerializableURI = @Serializable(with = JavaURISerializer::class) URI

object JavaURISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor("surfapi.JavaURI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URI {
        return URI.create(decoder.decodeString())
    }
}