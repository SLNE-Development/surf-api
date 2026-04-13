package dev.slne.surf.api.paper.display.element

import dev.slne.surf.api.paper.display.render.Canvas
import dev.slne.surf.api.paper.display.shape.Shape

/**
 * A container element (like HTML `<div>`) that can hold child elements.
 */
class Div : Element() {
    /** Add a child div container. */
    fun div(block: Div.() -> Unit = {}): Div {
        return Div().also { it.block(); children.add(it) }
    }

    /** Add a text label. */
    fun label(text: String, block: Label.() -> Unit = {}): Label {
        return Label(text).also { it.block(); children.add(it) }
    }

    /** Add an image element from a canvas source. */
    fun image(source: Canvas, block: ImageElement.() -> Unit = {}): ImageElement {
        return ImageElement(source).also { it.block(); children.add(it) }
    }

    /** Add a shape element. */
    fun shape(shape: Shape, block: ShapeElement.() -> Unit = {}): ShapeElement {
        return ShapeElement(shape).also { it.block(); children.add(it) }
    }
}
