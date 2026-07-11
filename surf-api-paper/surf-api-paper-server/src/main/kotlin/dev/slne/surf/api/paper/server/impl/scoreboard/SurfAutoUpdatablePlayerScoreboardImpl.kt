package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatablePlayerScoreboard
import dev.slne.surf.api.paper.util.forEachPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import java.util.UUID

class SurfAutoUpdatablePlayerScoreboardImpl(
    title: Component,
    maxLines: Int,
    sidebarComponent: SidebarComponent,
    animations: List<SidebarAnimation<Component>>
) : SurfAutoUpdatableScoreboardImpl(
    title, maxLines, sidebarComponent, animations
), SurfAutoUpdatablePlayerScoreboard {
    private val addedViewers = HashSet<UUID>()

    override fun addViewer(viewer: Player) {
        log
            .warn("You are not allowed to add viewers to this scoreboard. This Scoreboard automatically adds viewers.")
    }

    override fun removeViewer(viewer: Player) {
        log.warn(
            "You are not allowed to remove viewers from this scoreboard. This Scoreboard automatically removes viewers."
        )
    }

    override fun disable() {
        super.disable()
        addedViewers.clear()
    }

    override fun update() {
        super.update()
        val scoreboard = scoreboard ?: return
        addedViewers.removeIf { Bukkit.getPlayer(it) == null }
        forEachPlayer {
            if (addedViewers.add(it.uniqueId)) {
                scoreboard.addPlayer(it)
            }
        }
    }

    companion object {
        private val log = ComponentLogger.logger()
    }
}
