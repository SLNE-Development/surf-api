@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.api.serialization.adventure.book

import dev.slne.surf.api.serialization.adventure.component.AdventureComponentSerializer
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.*
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component

typealias SerializableBook = @Serializable(with = AdventureBookSerializer::class) Book

object AdventureBookSerializer : KSerializer<Book> {
    private val componentSerializer = ContextualSerializer(
        Component::class,
        AdventureComponentSerializer,
        emptyArray(),
    )

    private val pagesSerializer = ListSerializer(componentSerializer)

    override val descriptor = buildClassSerialDescriptor("surf.api.AdventureBook") {
        element("title", componentSerializer.descriptor)
        element("author", componentSerializer.descriptor)
        element("pages", pagesSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: Book,
    ) {
        encoder.encodeStructure(descriptor) {
            encodeSerializableElement(descriptor, 0, componentSerializer, value.title())
            encodeSerializableElement(descriptor, 1, componentSerializer, value.author())
            encodeSerializableElement(descriptor, 2, pagesSerializer, value.pages())
        }
    }

    override fun deserialize(decoder: Decoder): Book {
        return decoder.decodeStructure(descriptor) {
            var title: Component? = null
            var author: Component? = null
            var pages: List<Component>? = null

            if (decodeSequentially()) {
                title = decodeSerializableElement(descriptor, 0, componentSerializer)
                author = decodeSerializableElement(descriptor, 1, componentSerializer)
                pages = decodeSerializableElement(descriptor, 2, pagesSerializer)
            } else {
                while (true) {
                    when (val index = decodeElementIndex(descriptor)) {
                        0 -> title = decodeSerializableElement(descriptor, 0, componentSerializer)
                        1 -> author = decodeSerializableElement(descriptor, 1, componentSerializer)
                        2 -> pages = decodeSerializableElement(descriptor, 2, pagesSerializer)
                        CompositeDecoder.DECODE_DONE -> break
                        else -> error("Unexpected index: $index")
                    }
                }
            }

            Book.book(
                title ?: error("Missing title"),
                author ?: error("Missing author"),
                pages ?: error("Missing pages"),
            )
        }
    }
}