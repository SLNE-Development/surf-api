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
import java.util.function.Supplier

class SurfScoreboardBuilderImpl(private val title: Component) : SurfScoreboardBuilder {
    private val sidebarComponentBuilder = SidebarComponent.builder()
    private val animations = mutableObjectListOf<SidebarAnimation<Component>>()
    private var maxLines = SurfScoreboardBuilder.DEFAULT_MAX_LINES

    override fun maxLines(maxLines: Int) = apply {
        require(maxLines in 1..15) { "maxLines must be between 1 and 15" }
        this.maxLines = maxLines
    }

    override fun addLine(line: Component) = apply {
        sidebarComponentBuilder.addStaticLine(line)
    }

    override fun addUpdatableLine(line: Supplier<Component>) = apply {
        sidebarComponentBuilder.addDynamicLine(line)
    }

    override fun addAnimatedLine(animation: SidebarAnimation<SidebarComponent>) = apply {
        sidebarComponentBuilder.addAnimatedComponent(animation)
    }

    override fun addAnimatedLine(frames: MutableList<Component>) = apply {
        check(frames.isNotEmpty()) { "frames cannot be empty" }

        val animation = CollectionSidebarAnimation(frames)
        sidebarComponentBuilder.addAnimatedLine(animation)
        animations.add(animation)
    }

    override fun addGradientLine(text: Component, start: TextColor, end: TextColor) = apply {
        val gradient = createGradientAnimation(text, start.asHexString(), end.asHexString())
        sidebarComponentBuilder.addAnimatedLine(gradient)
        animations.add(gradient)
    }

    override fun build() = SurfScoreboardImpl(
        title, maxLines, sidebarComponentBuilder.build(), animations
    )


    override fun buildAutoUpdatable() = SurfAutoUpdatableScoreboardImpl(
        title,
        maxLines,
        sidebarComponentBuilder.build(),
        animations
    )


    override fun buildAutoUpdatablePlayer() = SurfAutoUpdatablePlayerScoreboardImpl(
        title,
        maxLines,
        sidebarComponentBuilder.build(),
        animations
    )


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