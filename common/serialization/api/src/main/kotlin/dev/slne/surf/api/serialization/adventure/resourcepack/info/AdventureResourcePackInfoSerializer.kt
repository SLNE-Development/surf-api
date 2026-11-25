@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.serialization.adventure.resourcepack.info

import dev.slne.surf.api.serialization.java.uri.URISerializer
import dev.slne.surf.api.serialization.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import net.kyori.adventure.resource.ResourcePackInfo
import java.net.URI
import java.util.*

typealias SerializableAdventureResourcePackInfo = @Serializable(
    with = AdventureResourcePackInfoSerializer::class,
) ResourcePackInfo

object AdventureResourcePackInfoSerializer : KSerializer<ResourcePackInfo> {
    private val uuidSerializer = ContextualSerializer(UUID::class, JavaUUIDSerializer, emptyArray())

    private val uriSerializer = ContextualSerializer(URI::class, URISerializer, emptyArray())

    override val descriptor = buildClassSerialDescriptor("surf.api.AdventureResourcePackInfo") {
        element("id", uuidSerializer.descriptor)
        element("uri", uriSerializer.descriptor)
        element<String>("hash")
    }

    override fun serialize(
        encoder: Encoder,
        value: ResourcePackInfo,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, uuidSerializer, value.id())
            encodeSerializableElement(descriptor, 1, uriSerializer, value.uri())
            encodeStringElement(descriptor, 2, value.hash())
        }
    }

    override fun deserialize(decoder: Decoder): ResourcePackInfo {
        return decoder.decodeStructure(descriptor) {
            var id: UUID? = null
            var uri: URI? = null
            var hash: String? = null

            if (decodeSequentially()) {
                id = decodeSerializableElement(descriptor, 0, uuidSerializer)
                uri = decodeSerializableElement(descriptor, 1, uriSerializer)
                hash = decodeStringElement(descriptor, 2)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeSerializableElement(descriptor, 0, uuidSerializer)
                        1 -> uri = decodeSerializableElement(descriptor, 1, uriSerializer)
                        2 -> hash = decodeStringElement(descriptor, 2)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }

            ResourcePackInfo.resourcePackInfo(
                id ?: error("Missing id"),
                uri ?: error("Missing uri"),
                hash ?: error("Missing hash"),
            )
        }
    }
}