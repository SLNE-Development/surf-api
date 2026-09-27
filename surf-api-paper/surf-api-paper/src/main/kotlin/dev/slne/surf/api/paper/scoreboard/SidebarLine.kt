package dev.slne.surf.api.paper.scoreboard

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.kyori.adventure.text.Component

/**
 * A single sidebar line.
 *
 * @property score text shown right-aligned in place of the score value, or `null` to show nothing
 */
data class SidebarLine(val text: Component, val score: Component? = null)

/**
 * Adds a line whose text is built with [SurfComponentBuilder].
 */
inline fun MutableList<in SidebarLine>.line(
    score: Component? = null,
    text: SurfComponentBuilder.() -> Unit
) {
    add(SidebarLine(SurfComponentBuilder(text), score))
}
