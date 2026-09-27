package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.paper.scoreboard.SurfScoreboardBuilder
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.CollectionSidebarAnimation
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.entity.Player
import java.util.function.Function
import java.util.function.Supplier

class SurfScoreboardBuilderImpl(private val title: Component) : SurfScoreboardBuilder {
    private val lines = mutableObjectListOf<ScoreboardLine>()
    private val snapshots = mutableObjectListOf<LineSnapshot<*>>()
    private val animations = mutableObjectListOf<SidebarAnimation<Component>>()
    private var maxLines = SurfScoreboardBuilder.DEFAULT_MAX_LINES

    override fun maxLines(maxLines: Int) = apply {
        require(maxLines in 1..15) { "maxLines must be between 1 and 15" }
        this.maxLines = maxLines
    }

    override fun addLine(line: Component) = addShared(SidebarComponent.staticLine(line))

    override fun addUpdatableLine(line: Supplier<Component>) = apply {
        val snapshot = LineSnapshot(line::get).also { snapshots.add(it) }
        addShared { drawable -> drawable.drawLine(snapshot.value) }
    }

    override fun addViewerComponent(component: Function<Player, SidebarComponent>) = apply {
        lines.add(ScoreboardLine.Viewer(component))
    }

    override fun addAnimatedLine(animation: SidebarAnimation<SidebarComponent>) =
        addShared(SidebarComponent.animatedComponent(animation))

    override fun addAnimatedLine(frames: MutableList<Component>) = apply {
        check(frames.isNotEmpty()) { "frames cannot be empty" }
        addAnimation(CollectionSidebarAnimation(frames))
    }

    override fun addGradientLine(text: Component, start: TextColor, end: TextColor) =
        addAnimation(createGradientAnimation(text, start.asHexString(), end.asHexString()))

    private fun addAnimation(animation: SidebarAnimation<Component>) = apply {
        animations.add(animation)
        addShared(SidebarComponent.animatedLine(animation))
    }

    private fun addShared(component: SidebarComponent) = apply {
        lines.add(ScoreboardLine.Shared(component))
    }

    private fun definition() = ScoreboardDefinition(
        title,
        maxLines,
        lines.toList(),
        snapshots.toList(),
        animations.toList(),
    )

    override fun build() = SurfScoreboardImpl(definition())
    override fun buildAutoUpdatable() = SurfAutoUpdatableScoreboardImpl(definition())
    override fun buildAutoUpdatablePlayer() = SurfAutoUpdatablePlayerScoreboardImpl(definition())

    companion object {
        private fun createGradientAnimation(
            component: Component,
            firstHex: String, secondHex: String
        ): SidebarAnimation<Component> {
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

            return CollectionSidebarAnimation(frames)
        }
    }
}
