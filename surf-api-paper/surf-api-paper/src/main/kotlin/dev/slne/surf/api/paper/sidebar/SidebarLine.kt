package dev.slne.surf.api.paper.sidebar

import net.kyori.adventure.text.Component

/**
 * Represents a single line displayed in a sidebar.
 *
 * @property text the main content of the line
 * @property score optional content displayed in the score area
 */
data class SidebarLine(val text: Component, val score: Component? = null)
