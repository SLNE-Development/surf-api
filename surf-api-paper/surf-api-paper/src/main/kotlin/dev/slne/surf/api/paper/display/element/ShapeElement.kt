package dev.slne.surf.api.paper.display.element

import dev.slne.surf.api.paper.display.shape.Shape

/**
 * An element that renders a geometric [Shape].
 * The shape handles its own pixel rasterization via [Shape.rasterize].
 */
class ShapeElement(val shape: Shape) : Element()
