package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.inventory.Book

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BookDsl

inline fun book(block: @BookDsl Book.Builder.() -> Unit): Book {
    return Book.builder().apply(block).build()
}

inline fun Book(block: @BookDsl Book.Builder.() -> Unit) = book(block)

inline fun Book.Builder.title(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    title(buildText(block))
}

inline fun Book.Builder.author(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    author(buildText(block))
}

inline fun Book.Builder.page(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    addPage(buildText(block))
}
