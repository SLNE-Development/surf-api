package dev.slne.surf.api.paper.scoreboard

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.megavex.scoreboardlibrary.api.objective.ScoreFormat
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent

/** Builds a [SidebarComponent] from the lines added in [block]. */
inline fun buildSidebarComponent(block: SidebarComponent.Builder.() -> Unit): SidebarComponent =
    SidebarComponent.builder().apply(block).build()

/** Adds a static line built with [SurfComponentBuilder]. */
inline fun SidebarComponent.Builder.buildStaticLine(
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addStaticLine(SurfComponentBuilder(line))

/**
 * Adds a static line built with [SurfComponentBuilder] and a custom score format.
 */
inline fun SidebarComponent.Builder.buildStaticLine(
    scoreFormat: ScoreFormat,
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addStaticLine(SurfComponentBuilder(line), scoreFormat)

/** Adds a line that is rebuilt with [SurfComponentBuilder] every time the component is drawn. */
fun SidebarComponent.Builder.buildDynamicLine(
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addDynamicLine { SurfComponentBuilder(line) }
