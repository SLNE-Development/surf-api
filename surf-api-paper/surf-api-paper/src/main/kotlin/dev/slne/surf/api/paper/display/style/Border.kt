package dev.slne.surf.api.paper.display.style

import dev.slne.surf.api.paper.display.argb

/**
 * Border definition with width and color.
 */
data class Border(
    val width: Int = 1,
    val color: Int = argb(0x000000)
)
