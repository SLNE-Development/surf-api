package dev.slne.surf.surfapi.core.api.messages.adventure

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder
import net.kyori.adventure.inventory.Book

/**
 * A DSL marker for the Book DSL to prevent scope conflicts in nested DSL blocks.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class BookDsl

/**
 * Creates a [Book] using the DSL-style builder.
 *
 * @param block The configuration block for building the book.
 * @return A configured [Book] instance.
 *
 * **Example Usage:**
 * ```kotlin
 * val myBook = book {
 *     title { appendText("The Great Adventure") }
 *     author { appendText("John Doe") }
 *     page { appendText("Once upon a time...") }
 *     page { appendText("The journey continues...") }
 * }
 * ```
 */
inline fun book(block: @BookDsl Book.Builder.() -> Unit): Book {
    return Book.builder().apply(block).build()
}

/**
 * Creates a [Book] using the DSL-style builder. This is an alias for [book].
 *
 * @param block The configuration block for building the book.
 * @return A configured [Book] instance.
 */
inline fun Book(block: @BookDsl Book.Builder.() -> Unit) = book(block)

/**
 * Sets the title of the book using a component builder.
 *
 * @param block The configuration block for creating the title component.
 *
 * **Example Usage:**
 * ```kotlin
 * book {
 *     title { appendText("My Story", PRIMARY) }
 * }
 * ```
 */
inline fun Book.Builder.title(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    title(buildText(block))
}

/**
 * Sets the author of the book using a component builder.
 *
 * @param block The configuration block for creating the author component.
 *
 * **Example Usage:**
 * ```kotlin
 * book {
 *     author { appendText("Jane Doe", SECONDARY) }
 * }
 * ```
 */
inline fun Book.Builder.author(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    author(buildText(block))
}

/**
 * Adds a new page to the book using a component builder.
 *
 * @param block The configuration block for creating the page content.
 *
 * **Example Usage:**
 * ```kotlin
 * book {
 *     page { appendText("Chapter 1: The Beginning") }
 *     page { appendText("Chapter 2: The Journey") }
 * }
 * ```
 */
inline fun Book.Builder.page(block: @BookDsl SurfComponentBuilder.() -> Unit) {
    addPage(buildText(block))
}
