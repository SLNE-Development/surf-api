package dev.slne.surf.api.paper.server.impl.scoreboard

import net.kyori.adventure.text.Component
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.entity.Player
import java.util.function.Function

sealed interface ScoreboardLine {
    class Shared(val component: SidebarComponent) : ScoreboardLine
    class Viewer(val factory: Function<Player, SidebarComponent>) : ScoreboardLine
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

class ScoreboardDefinition(
    val title: Component,
    val maxLines: Int,
    val lines: List<ScoreboardLine>,
    val snapshots: List<LineSnapshot<*>>,
    val animations: List<SidebarAnimation<Component>>
)
