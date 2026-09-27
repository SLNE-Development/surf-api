package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.paper.scoreboard.SidebarLine
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.entity.Player
import java.util.function.Function
import java.util.function.Supplier

class SurfScoreboardBuilderImpl(private val title: Component) : SurfScoreboardBuilder {
    private val lines = ObjectArrayList<ScoreboardLine>()
    private val snapshots = ObjectArrayList<LineSnapshot<*>>()
    private val animations = ObjectArrayList<FrameAnimation>()
    private var maxLines = SurfScoreboardBuilder.DEFAULT_MAX_LINES

    override fun maxLines(maxLines: Int) = apply {
        require(maxLines in 1..15) { "maxLines must be between 1 and 15" }
        this.maxLines = maxLines
    }

    override fun addLine(line: Component) = addShared { it += SidebarLine(line) }

    override fun addUpdatableLine(line: Supplier<Component>) = apply {
        val snapshot = LineSnapshot(line::get).also { snapshots.add(it) }
        addShared { it += SidebarLine(snapshot.value) }
    }

    override fun addViewerLines(lines: Function<Player, List<SidebarLine>>) = apply {
        this.lines.add(ScoreboardLine.Viewer(lines))
    }

    @Deprecated("Use addViewerLines instead", ReplaceWith("addViewerLines(component)"))
    override fun addViewerComponent(component: Function<Player, SidebarComponent>) =
        addViewerLines { viewer -> buildList { component.apply(viewer).drawTo(this) } }

    @Deprecated("Use addAnimatedLine(frames) instead", ReplaceWith("addAnimatedLine(frames)"))
    override fun addAnimatedLine(animation: SidebarAnimation<SidebarComponent>) =
        addShared { animation.currentFrame().drawTo(it) }

    override fun addAnimatedLine(frames: MutableList<Component>) = apply {
        check(frames.isNotEmpty()) { "frames cannot be empty" }
        addAnimation(FrameAnimation(frames))
    }

    override fun addGradientLine(text: Component, start: TextColor, end: TextColor) =
        addAnimation(createGradientAnimation(text, start.asHexString(), end.asHexString()))

    private fun addAnimation(animation: FrameAnimation) = apply {
        animations.add(animation)
        addShared { it += SidebarLine(animation.currentFrame) }
    }

    private fun addShared(line: ScoreboardLine.Shared) = apply {
        lines.add(line)
    }

    private fun definition() = ScoreboardDefinition(
        title,
        maxLines,
        lines.clone(),
        snapshots.clone(),
        animations.clone(),
    )

    override fun build() = SurfScoreboardImpl(definition())
    override fun buildAutoUpdatable() = SurfAutoUpdatableScoreboardImpl(definition())
    override fun buildAutoUpdatablePlayer() = SurfAutoUpdatablePlayerScoreboardImpl(definition())

    companion object {
        private fun createGradientAnimation(
            component: Component,
            firstHex: String, secondHex: String
        ): FrameAnimation {
            val step = 1f / 20f
            val textPlaceholder = Placeholder.component("text", component)
            val frames = mutableObjectListOf<Component>()

            // Animation from left to right
            var phase = -1f
            while (phase < 1) {
                frames.add(
                    MiniMessage.miniMessage().deserialize(
                        "<gradient:$firstHex:$secondHex:$phase><text></gradient>",
                        textPlaceholder
                    )
                )
                phase += step
            }

            // Animation from right to left
            frames.addAll(frames.reversed())

            return FrameAnimation(frames)
        }
    }
}
