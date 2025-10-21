package dev.slne.surf.surfapi.core.api.serializer.adventure.book

import com.mojang.serialization.Codec
import com.mojang.serialization.codecs.RecordCodecBuilder
import dev.slne.surf.surfapi.core.api.serializer.adventure.component.AdventureComponentCodec
import net.kyori.adventure.inventory.Book

object AdventureBookCodec {
    val CODEC: Codec<Book> = RecordCodecBuilder.create { instance ->
        instance.group(
            AdventureComponentCodec.CODEC.fieldOf("title").forGetter(Book::title),
            AdventureComponentCodec.CODEC.fieldOf("author").forGetter(Book::author),
            AdventureComponentCodec.CODEC.listOf()
                .optionalFieldOf("pages", emptyList())
                .forGetter(Book::pages),
        ).apply(instance) { title, author, pages ->
            Book.book(title, author, *pages.toTypedArray())
        }
    }
}