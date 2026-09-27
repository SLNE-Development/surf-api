package dev.slne.surf.api.paper.sidebar

import dev.slne.surf.api.core.messages.Colors
import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.jetbrains.annotations.Range
import kotlin.time.Duration

@DslMarker
annotation class SidebarDsl

/**
 * Configures the title and lines of a [SurfSidebar].
 *
 * Lines are displayed in the order in which they are added. Viewer-dependent
 * renderers are evaluated asynchronously for each viewer whenever the sidebar
 * is updated. See [SurfSidebar] for the threading contract.
 *
 * If a renderer fails, the failure is logged and the affected viewer keeps
 * their previously rendered content.
 *
 * @see SurfSidebar
 */
@SidebarDsl
interface SidebarBuilder {

    /**
     * The maximum number of lines displayed by the sidebar.
     *
     * Additional lines are discarded.
     */
    var maxLines: @Range(from = 1, to = 15) Int

    /**
     * Sets the title displayed by the sidebar.
     *
     * @param title the title to display
     */
    fun title(title: Component)

    /**
     * Sets a title rendered asynchronously for each viewer on every update.
     *
     * @param render produces the title for the given viewer
     */
    fun title(render: suspend (viewer: Player) -> Component)

    /**
     * Adds a static line to the sidebar.
     *
     * @param text the line content
     * @param score optional content displayed in the score area
     */
    fun line(text: Component, score: Component? = null)

    /**
     * Adds a line rendered asynchronously for each viewer on every update.
     *
     * @param render produces the line content for the given viewer
     */
    fun line(render: suspend (viewer: Player) -> Component)

    /**
     * Adds a line whose content and score are rendered asynchronously for each viewer.
     *
     * @param render produces the line for the given viewer
     */
    fun scoredLine(render: suspend (viewer: Player) -> SidebarLine)

    /**
     * Adds a dynamic collection of lines rendered asynchronously for each viewer.
     *
     * The returned number of lines may vary between viewers and between updates.
     *
     * @param render produces the lines for the given viewer
     */
    fun lines(render: suspend (viewer: Player) -> List<SidebarLine>)

    /**
     * Adds an empty line to the sidebar.
     */
    fun emptyLine() {
        line(Component.empty())
    }

    /**
     * Adds an animated line that advances through [frames] on each update.
     *
     * @param frames the animation frames
     */
    fun animatedLine(frames: List<Component>)

    /**
     * Adds a line with an animated gradient applied across [text].
     *
     * The gradient moves between [start] and [end] as the sidebar updates.
     *
     * @param text the content to apply the gradient to
     * @param start the starting gradient color
     * @param end the ending gradient color
     */
    fun gradientLine(text: Component, start: TextColor, end: TextColor)

    /**
     * Adds the default gradient separator line.
     */
    fun separator() {
        gradientLine(Component.text("--------------------"), Colors.WHITE, Colors.SPACER)
    }

    /**
     * Sets a static title built using [SurfComponentBuilder].
     *
     * @param block configures the title component
     */
    fun buildTitle(block: SurfComponentBuilder.() -> Unit) {
        title(SurfComponentBuilder(block))
    }

    /**
     * Sets a title built asynchronously for each viewer using [SurfComponentBuilder].
     *
     * @param block configures the title for the given viewer
     */
    fun buildViewerTitle(block: suspend SurfComponentBuilder.(viewer: Player) -> Unit) {
        title { viewer -> SurfComponentBuilder { block(viewer) } }
    }

    /**
     * Adds a static line built using [SurfComponentBuilder].
     *
     * @param score optional content displayed in the score area
     * @param text configures the line content
     */
    fun buildLine(score: Component? = null, text: SurfComponentBuilder.() -> Unit) {
        line(SurfComponentBuilder(text), score)
    }

    /**
     * Adds a line built asynchronously for each viewer using [SurfComponentBuilder].
     *
     * @param text configures the line for the given viewer
     */
    fun buildViewerLine(text: suspend SurfComponentBuilder.(viewer: Player) -> Unit) {
        line { viewer -> SurfComponentBuilder { text(viewer) } }
    }

    /**
     * Adds a dynamic collection of lines built asynchronously for each viewer.
     *
     * @param block collects the lines for the given viewer
     */
    fun buildViewerLines(block: suspend SidebarLinesBuilder.(viewer: Player) -> Unit) {
        lines { viewer -> SidebarLinesBuilder().apply { block(viewer) }.build() }
    }

    /**
     * Adds a gradient line whose content is built using [SurfComponentBuilder].
     *
     * @param start the starting gradient color
     * @param end the ending gradient color
     * @param text configures the line content
     */
    fun buildGradientLine(start: TextColor, end: TextColor, text: SurfComponentBuilder.() -> Unit) {
        gradientLine(SurfComponentBuilder(text), start, end)
    }
}

/**
 * Collects lines for [SidebarBuilder.buildViewerLines].
 */
@SidebarDsl
class SidebarLinesBuilder internal constructor() {
    private val lines = ObjectArrayList<SidebarLine>()

    /**
     * Adds [line] to the collected sidebar lines.
     *
     * @param line the line to add
     */
    fun line(line: SidebarLine) {
        lines.add(line)
    }

    /**
     * Adds a line with optional score content.
     *
     * @param text the line content
     * @param score optional content displayed in the score area
     */
    fun line(text: Component, score: Component? = null) {
        line(SidebarLine(text, score))
    }

    /**
     * Adds a line built using [SurfComponentBuilder].
     *
     * @param score optional content displayed in the score area
     * @param text configures the line content
     */
    inline fun buildLine(score: Component? = null, text: SurfComponentBuilder.() -> Unit) {
        line(SurfComponentBuilder(text), score)
    }

    /**
     * Adds a line whose content and score are both built using [SurfComponentBuilder].
     *
     * @param text configures the line content
     * @param score configures the score content
     */
    inline fun buildScoredLine(
        text: SurfComponentBuilder.() -> Unit,
        score: SurfComponentBuilder.() -> Unit
    ) {
        line(SurfComponentBuilder(text), SurfComponentBuilder(score))
    }

    /**
     * Adds an empty line.
     */
    fun emptyLine() {
        line(Component.empty())
    }

    internal fun build(): List<SidebarLine> = lines
}

/**
 * Configures a [SurfSidebar] that updates automatically at a fixed interval.
 */
@SidebarDsl
interface AutoUpdatingSidebarBuilder : SidebarBuilder {
    /**
     * The interval between automatic sidebar updates.
     *
     * The interval must be at least one tick.
     *
     * @see io.papermc.paper.util.Tick
     */
    var updateInterval: Duration
}
