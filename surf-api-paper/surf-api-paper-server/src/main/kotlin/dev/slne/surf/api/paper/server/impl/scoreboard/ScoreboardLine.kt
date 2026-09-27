package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.paper.scoreboard.SidebarLine
import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.objective.ScoreFormat
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import org.bukkit.entity.Player
import java.util.function.Function

sealed interface ScoreboardLine {
    fun interface Shared : ScoreboardLine {
        fun draw(lines: MutableList<SidebarLine>)
    }

    class Viewer(val factory: Function<Player, List<SidebarLine>>) : ScoreboardLine
}

/**
 * Holds the value of a mutable line source as of the last [refresh].
 */
class LineSnapshot<T : Any>(private val source: () -> T) {
    lateinit var value: T
        private set

    fun refresh() {
        value = source()
    }
}

class FrameAnimation(frames: List<Component>) {
    private val frames = frames.toTypedArray()
    private var index = 0

    val currentFrame: Component
        get() = frames[index]

    fun nextFrame() {
        index = (index + 1) % frames.size
    }

    fun reset() {
        index = 0
    }
}

fun SidebarComponent.drawTo(lines: MutableList<SidebarLine>) {
    draw { line, scoreFormat ->
        lines += SidebarLine(line.asComponent(), (scoreFormat as? ScoreFormat.Fixed)?.content())
    }
}

class ScoreboardDefinition(
    val title: Component,
    val maxLines: Int,
    val lines: List<ScoreboardLine>,
    val snapshots: List<LineSnapshot<*>>,
    val animations: List<FrameAnimation>
)
