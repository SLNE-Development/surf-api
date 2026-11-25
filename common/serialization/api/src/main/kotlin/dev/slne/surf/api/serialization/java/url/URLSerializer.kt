package dev.slne.surf.api.serialization.java.url

import dev.slne.surf.api.serialization.java.uri.URISerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.net.URI
import java.net.URL

typealias SerializableURL = @Serializable(with = URLSerializer::class) URL

/**
 * Serializes a [URL] as its [URI] through the format's contextual [URI] serializer, with
 * [URISerializer] as the fallback.
 *
 * [descriptor] always describes the fallback encoding. A format that registers its own
 * [URI] serializer may write a different shape, which descriptor-driven checks such as
 * Json's map-key validation do not see.
 */
object URLSerializer : KSerializer<URL> {
    private val uriSerializer = ContextualSerializer(URI::class, URISerializer, emptyArray())

    override val descriptor: SerialDescriptor = SerialDescriptor(
        "surf.api.JavaURL",
        URISerializer.descriptor,
    )

    override fun serialize(encoder: Encoder, value: URL) {
        encoder.encodeSerializableValue(uriSerializer, value.toURI())
    }

    override fun deserialize(decoder: Decoder): URL {
        return decoder.decodeSerializableValue(uriSerializer).toURL()
    }
}