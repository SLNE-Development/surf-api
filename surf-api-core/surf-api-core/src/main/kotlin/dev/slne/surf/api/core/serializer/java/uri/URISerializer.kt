@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.java.uri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI

typealias SerializableURI = @Serializable(with = URISerializer::class) URI

object URISerializer : KSerializer<URI> {
    override val descriptor = PrimitiveSerialDescriptor("surf.api.JavaURI", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: URI) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): URI {
        return URI.create(decoder.decodeString())
    }
}