package dev.slne.surf.api.paper.display.document

import dev.slne.surf.api.paper.display.element.Div
import dev.slne.surf.api.paper.display.render.Canvas
import dev.slne.surf.api.paper.display.render.Renderer

/**
 * A document represents the root of a display's element tree.
 * It has a fixed pixel size and contains a root [Div] element.
 *
 * Usage:
 * ```kotlin
 * val doc = document(384, 256) {
 *     style { backgroundColor = color(0x1E1E2E) }
 *     div {
 *         label("Hello World!") {
 *             style { color = color(0xFFFFFF) }
 *         }
 *     }
 * }
 * val canvas = doc.render()
 * ```
 */
class Document(val width: Int, val height: Int) {
    val root = Div().apply {
        style.width = width
        style.height = height
    }

    /**
     * Renders the element tree into a [Canvas].
     */
    fun render(): Canvas {
        val canvas = Canvas(width, height)
        Renderer.render(root, canvas)
        return canvas
    }
}

/**
 * DSL entry point for creating a [Document].
 */
fun document(width: Int, height: Int, block: Div.() -> Unit): Document {
    val doc = Document(width, height)
    doc.root.block()
    return doc
}
