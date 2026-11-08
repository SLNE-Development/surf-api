package dev.slne.surf.api.core.serializer.java.url

import dev.slne.surf.api.core.serializer.java.uri.URISerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URL

typealias SerializableURL = @Serializable(with = URLSerializer::class) URL

object URLSerializer : KSerializer<URL> {
    override val descriptor = SerialDescriptor("surf.api.JavaURL", URISerializer.descriptor)

    override fun serialize(encoder: Encoder, value: URL) {
        URISerializer.serialize(encoder, value.toURI())
    }

    override fun deserialize(decoder: Decoder): URL {
        return URISerializer.deserialize(decoder).toURL()
    }
}