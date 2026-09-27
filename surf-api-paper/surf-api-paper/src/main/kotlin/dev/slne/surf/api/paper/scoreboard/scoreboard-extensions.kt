package dev.slne.surf.api.paper.scoreboard

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import net.megavex.scoreboardlibrary.api.objective.ScoreFormat
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent

/** Builds a [SidebarComponent] from the lines added in [block]. */
@Deprecated(
    "SidebarComponent belongs to the legacy scoreboard. In the sidebar API, collect lines per " +
    "viewer with buildViewerLines { viewer -> buildLine { ... } } in the sidebar builder.",
    ReplaceWith("buildList(block)")
)
inline fun buildSidebarComponent(block: SidebarComponent.Builder.() -> Unit): SidebarComponent =
    SidebarComponent.builder().apply(block).build()

/** Adds a static line built with [SurfComponentBuilder]. */
@Deprecated(
    "Inside buildViewerLines { } or buildSharedLines { } of the sidebar API, " +
    "use buildLine { ... } instead.",
    ReplaceWith("buildLine(text = line)")
)
inline fun SidebarComponent.Builder.buildStaticLine(
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addStaticLine(SurfComponentBuilder(line))

/**
 * Adds a static line built with [SurfComponentBuilder] and a custom score format.
 */
@Deprecated(
    "Inside buildViewerLines { } or buildSharedLines { } of the sidebar API, " +
    "use buildLine(score) { ... } instead. The score is a plain Component there: " +
    "ScoreFormat.fixed(content) corresponds to score = content.",
    ReplaceWith(
        "buildLine(score = (scoreFormat as? ScoreFormat.Fixed)?.content(), text = line)",
        "net.megavex.scoreboardlibrary.api.objective.ScoreFormat"
    )
)
inline fun SidebarComponent.Builder.buildStaticLine(
    scoreFormat: ScoreFormat,
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addStaticLine(SurfComponentBuilder(line), scoreFormat)

/** Adds a line that is rebuilt with [SurfComponentBuilder] every time the component is drawn. */
@Deprecated(
    "Viewer lines of the sidebar API are rebuilt on every update anyway. " +
    "Inside buildViewerLines { } use buildLine { ... } instead.",
    ReplaceWith("buildLine(text = line)")
)
fun SidebarComponent.Builder.buildDynamicLine(
    line: SurfComponentBuilder.() -> Unit
): SidebarComponent.Builder = addDynamicLine { SurfComponentBuilder(line) }
