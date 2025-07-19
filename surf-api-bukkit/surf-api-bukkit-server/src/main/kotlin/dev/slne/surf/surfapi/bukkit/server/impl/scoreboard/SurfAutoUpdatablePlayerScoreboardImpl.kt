package dev.slne.surf.surfapi.bukkit.server.impl.scoreboard

import dev.slne.surf.surfapi.bukkit.api.scoreboard.ObsoleteScoreboardApi
import dev.slne.surf.surfapi.bukkit.api.scoreboard.SurfAutoUpdatablePlayerScoreboard
import dev.slne.surf.surfapi.bukkit.api.util.forEachPlayer
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent
import net.megavex.scoreboardlibrary.api.sidebar.component.animation.SidebarAnimation
import org.bukkit.entity.Player

@Suppress("OVERRIDE_DEPRECATION")
@ObsoleteScoreboardApi
class SurfAutoUpdatablePlayerScoreboardImpl(
    title: Component,
    maxLines: Int,
    sidebarComponent: SidebarComponent,
    animations: List<SidebarAnimation<Component>>
) : SurfAutoUpdatableScoreboardImpl(title, maxLines, sidebarComponent, animations),
    SurfAutoUpdatablePlayerScoreboard {

    override fun addViewer(viewer: Player) {
        ComponentLogger.logger()
            .warn("You are not allowed to add viewers to this scoreboard. This Scoreboard automatically adds viewers.")
    }

    override fun removeViewer(viewer: Player) {
        ComponentLogger.logger().warn(
            "You are not allowed to remove viewers from this scoreboard. This Scoreboard automatically removes viewers."
        )
    }

    override fun update() {
        super.update()
        val scoreboard = scoreboard ?: return
        forEachPlayer { scoreboard.addPlayer(it) }
    }
}
