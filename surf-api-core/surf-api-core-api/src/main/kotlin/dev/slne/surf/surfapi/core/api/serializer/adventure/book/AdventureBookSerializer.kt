@file:OptIn(ExperimentalSerializationApi::class)

package dev.slne.surf.surfapi.core.api.serializer.adventure.book

import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentSerializer
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
    private val pagesSerializer = ListSerializer(AdventureComponentSerializer)

    override val descriptor = buildClassSerialDescriptor("surfapi.AdventureBook") {
        element("title", AdventureComponentSerializer.descriptor)
        element("author", AdventureComponentSerializer.descriptor)
        element("pages", pagesSerializer.descriptor)
    }

    override fun serialize(
        encoder: Encoder,
        value: Book,
    ) = encoder.encodeStructure(descriptor) {
        encodeSerializableElement(descriptor, 0, AdventureComponentSerializer, value.title())
        encodeSerializableElement(descriptor, 1, AdventureComponentSerializer, value.author())
        encodeSerializableElement(descriptor, 2, pagesSerializer, value.pages())
    }

    override fun deserialize(decoder: Decoder): Book = decoder.decodeStructure(descriptor) {
        var title: Component? = null
        var author: Component? = null
        var pages: List<Component>? = null

        if (decodeSequentially()) {
            title = decodeSerializableElement(descriptor, 0, AdventureComponentSerializer)
            author = decodeSerializableElement(descriptor, 1, AdventureComponentSerializer)
            pages = decodeSerializableElement(descriptor, 2, pagesSerializer)
        } else {
            while (true) {
                when (val index = decodeElementIndex(descriptor)) {
                    0 -> title =
                        decodeSerializableElement(descriptor, 0, AdventureComponentSerializer)

                    1 -> author =
                        decodeSerializableElement(descriptor, 1, AdventureComponentSerializer)

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