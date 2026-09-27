package dev.slne.surf.api.paper.server.impl.sidebar

import dev.slne.surf.api.paper.sidebar.AutoUpdatingSidebarBuilder
import dev.slne.surf.api.paper.sidebar.SidebarBuilder
import dev.slne.surf.api.paper.sidebar.SidebarLine
import io.papermc.paper.util.Tick
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration
import kotlin.time.toKotlinDuration

sealed interface SidebarTitle {
    class Static(val title: Component) : SidebarTitle
    class Rendered(val render: suspend (Player) -> Component) : SidebarTitle
}

sealed interface SidebarEntry {
    class Static(val line: SidebarLine) : SidebarEntry
    class Line(val render: suspend (Player) -> SidebarLine) : SidebarEntry
    class Lines(val render: suspend (Player) -> List<SidebarLine>) : SidebarEntry
    class Animated(val animation: FrameAnimation) : SidebarEntry
}

class FrameAnimation(frames: List<Component>) {
    private val frames = Array(frames.size) { SidebarLine(frames[it]) }
    private val index = AtomicInteger()

    val currentFrame: SidebarLine
        get() = frames[index.get()]

    fun nextFrame() {
        index.updateAndGet { (it + 1) % frames.size }
    }
}

class SidebarDefinition(
    val title: SidebarTitle,
    val entries: List<SidebarEntry>,
    val animations: List<FrameAnimation>,
    val maxLines: Int,
)

open class SidebarBuilderImpl : SidebarBuilder {
    private var title: SidebarTitle = SidebarTitle.Static(Component.empty())
    private val entries = ObjectArrayList<SidebarEntry>()
    private val animations = ObjectArrayList<FrameAnimation>()

    override var maxLines: Int = MAX_LINES
        set(value) {
            require(value in 1..MAX_LINES) { "maxLines must be between 1 and $MAX_LINES: $value" }
            field = value
        }

    override fun title(title: Component) {
        this.title = SidebarTitle.Static(title)
    }

    override fun title(render: suspend (viewer: Player) -> Component) {
        this.title = SidebarTitle.Rendered(render)
    }

    override fun line(text: Component, score: Component?) {
        entries.add(SidebarEntry.Static(SidebarLine(text, score)))
    }

    override fun line(render: suspend (viewer: Player) -> Component) {
        entries.add(SidebarEntry.Line { viewer -> SidebarLine(render(viewer)) })
    }

    override fun scoredLine(render: suspend (viewer: Player) -> SidebarLine) {
        entries.add(SidebarEntry.Line(render))
    }

    override fun lines(render: suspend (viewer: Player) -> List<SidebarLine>) {
        entries.add(SidebarEntry.Lines(render))
    }

    override fun animatedLine(frames: List<Component>) {
        require(frames.isNotEmpty()) { "frames must not be empty" }
        addAnimation(FrameAnimation(frames))
    }

    override fun gradientLine(text: Component, start: TextColor, end: TextColor) {
        addAnimation(FrameAnimation(gradientFrames(text, start.asHexString(), end.asHexString())))
    }

    private fun addAnimation(animation: FrameAnimation) {
        animations.add(animation)
        entries.add(SidebarEntry.Animated(animation))
    }

    fun definition() = SidebarDefinition(title, entries.clone(), animations.clone(), maxLines)

    companion object {
        const val MAX_LINES = 15

        private fun gradientFrames(text: Component, firstHex: String, secondHex: String): List<Component> {
            val step = 1f / 20f
            val textPlaceholder = Placeholder.component("text", text)
            val frames = ObjectArrayList<Component>()

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
            return frames
        }
    }
}

class AutoUpdatingSidebarBuilderImpl : SidebarBuilderImpl(), AutoUpdatingSidebarBuilder {
    override var updateInterval: Duration = DEFAULT_UPDATE_INTERVAL
        set(value) {
            require(value >= ONE_TICK) { "updateInterval must be at least one tick: $value" }
            field = value
        }

    companion object {
        private val ONE_TICK = Tick.of(1).toKotlinDuration()
        private val DEFAULT_UPDATE_INTERVAL = Tick.of(5).toKotlinDuration()
    }
}
