@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.core.serializer.adventure.resourcepack.info

import dev.slne.surf.api.core.serializer.java.uri.URISerializer
import dev.slne.surf.api.core.serializer.java.uuid.JavaUUIDSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.*
import net.kyori.adventure.resource.ResourcePackInfo
import java.net.URI
import java.util.*

typealias SerializableAdventureResourcePackInfo = @Serializable(with = AdventureResourcePackInfoSerializer::class) ResourcePackInfo

object AdventureResourcePackInfoSerializer : KSerializer<ResourcePackInfo> {
    override val descriptor = buildClassSerialDescriptor("surf.api.AdventureResourcePackInfo") {
        element("id", JavaUUIDSerializer.descriptor)
        element("uri", URISerializer.descriptor)
        element<String>("hash")
    }

    override fun serialize(
        encoder: Encoder,
        value: ResourcePackInfo,
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, JavaUUIDSerializer, value.id())
        encodeSerializableElement(descriptor, 1, URISerializer, value.uri())
        encodeStringElement(descriptor, 2, value.hash())
    }

    override fun deserialize(decoder: Decoder): ResourcePackInfo =
        decoder.decodeStructure(descriptor) {
            var id: UUID? = null
            var uri: URI? = null
            var hash: String? = null

            if (decodeSequentially()) {
                id = decodeSerializableElement(descriptor, 0, JavaUUIDSerializer)
                uri = decodeSerializableElement(descriptor, 1, URISerializer)
                hash = decodeStringElement(descriptor, 2)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> id = decodeSerializableElement(descriptor, 0, JavaUUIDSerializer)
                        1 -> uri = decodeSerializableElement(descriptor, 1, URISerializer)
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