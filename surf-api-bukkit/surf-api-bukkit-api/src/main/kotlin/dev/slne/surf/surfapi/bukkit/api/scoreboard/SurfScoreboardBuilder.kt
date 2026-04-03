package dev.slne.surf.surfapi.bukkit.api.scoreboard

import dev.slne.surf.surfapi.bukkit.api.SurfApiBukkit
import dev.slne.surf.surfapi.core.api.messages.Colors
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.jetbrains.annotations.Range
import java.util.function.Supplier

/**
 * The SurfScoreboardBuilder interface provides methods to build a SurfScoreboard, a
 * SurfAutoUpdatableScoreboard, or a SurfAutoUpdatablePlayerScoreboard.
 */
@ObsoleteScoreboardApi
interface SurfScoreboardBuilder {
    /**
     * Sets the maximum number of lines for the scoreboard.
     *
     * @param maxLines the maximum number of lines for the scoreboard
     * @return the SurfScoreboardBuilder object
     * @throws IllegalArgumentException if maxLines is not within the range of 1 to 15 (inclusive)
     */
    fun maxLines(maxLines: @Range(from = 1, to = 15) Int): SurfScoreboardBuilder

    /**
     * Adds a line to the SurfScoreboardBuilder.
     *
     * @param line the line to be added to the SurfScoreboardBuilder
     * @return the SurfScoreboardBuilder with the added line
     */
    fun addLine(line: Component): SurfScoreboardBuilder

    /**
     * Adds an empty line to the SurfScoreboardBuilder.
     *
     * @return the updated SurfScoreboardBuilder
     */
    fun addEmptyLine(): SurfScoreboardBuilder {
        return addLine(Component.empty())
    }

    /**
     * Adds an updatable line to the scoreboard. An updatable line is a line that can be dynamically
     * updated by supplying a Supplier that returns the updated content.
     *
     * @param line the Supplier that provides the updated content for the line
     * @return the SurfScoreboardBuilder instance
     * @throws NullPointerException if the line parameter is null
     */
    fun addUpdatableLine(line: Supplier<Component>): SurfScoreboardBuilder

    /**
     * Adds an animated line to the SurfScoreboardBuilder.
     *
     * @param animation the animation to be added as an animated line
     * @return the SurfScoreboardBuilder with the added animated line
     * @throws NullPointerException if `animation` is null
     */
    fun addAnimatedLine(
        animation: SidebarAnimation<SidebarComponent>
    ): SurfScoreboardBuilder

    /**
     * Adds an animated line to the scoreboard. The animated line will display a series of frames,
     * where each frame is a component. The frames will be displayed consecutively in an animated
     * manner.
     *
     * @param frames the list of frames to display as an animated line
     * @return the SurfScoreboardBuilder instance
     */
    fun addAnimatedLine(frames: MutableList<Component>): SurfScoreboardBuilder

    /**
     * Adds a gradient line to the SurfScoreboardBuilder. The gradient color starts with the specified
     * start color and gradually transitions to the specified end color.
     *
     * @param text  the text of the gradient line
     * @param start the start color of the gradient
     * @param end   the end color of the gradient
     * @return the SurfScoreboardBuilder instance with the added gradient line
     */
    fun addGradientLine(
        text: Component, start: TextColor,
        end: TextColor
    ): SurfScoreboardBuilder

    /**
     * Adds a line separator to the SurfScoreboardBuilder. This method adds a gradient line separator
     * composed of '-' characters to the SurfScoreboardBuilder. The default colors for the gradient
     * are Colors.WHITE and Colors.SPACER.
     *
     * @return a new SurfScoreboardBuilder instance with the added line separator
     */
    fun addLineSeparator(): SurfScoreboardBuilder {
        return addGradientLine(Component.text("--------------------"), Colors.WHITE, Colors.SPACER)
    }

    /**
     * Builds a SurfScoreboard.
     *
     * @return a [SurfScoreboard] instance
     */
    fun build(): SurfScoreboard

    /**
     * Builds an auto-updatable scoreboard.
     *
     * @return The built SurfAutoUpdatableScoreboard.
     */
    fun buildAutoUpdatable(): SurfAutoUpdatableScoreboard

    /**
     * Builds an auto-updatable player scoreboard.
     *
     * @return the built SurfAutoUpdatablePlayerScoreboard
     */
    fun buildAutoUpdatablePlayer(): SurfAutoUpdatablePlayerScoreboard

    companion object {
        /**
         * Builds a SurfScoreboard or its variations.
         *
         * @param title the title of the scoreboard
         * @return a SurfScoreboardBuilder with the given title
         */
        @JvmStatic
        fun builder(title: Component): SurfScoreboardBuilder {
            return SurfApiBukkit.createScoreboard(title)
        }

        /**
         * The maximum number of lines that a SurfScoreboard can have by default.
         */
        const val DEFAULT_MAX_LINES: Int = 15
    }
}
