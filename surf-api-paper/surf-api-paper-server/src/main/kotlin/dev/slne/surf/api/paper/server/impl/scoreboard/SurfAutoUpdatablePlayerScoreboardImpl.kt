package dev.slne.surf.api.paper.server.impl.scoreboard

import dev.slne.surf.api.paper.scoreboard.SurfAutoUpdatablePlayerScoreboard
import dev.slne.surf.api.paper.util.forEachPlayer
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.entity.Player

class SurfAutoUpdatablePlayerScoreboardImpl(
    definition: ScoreboardDefinition
) : SurfAutoUpdatableScoreboardImpl(definition), SurfAutoUpdatablePlayerScoreboard {
    override fun addViewer(viewer: Player) {
        ComponentLogger.logger()
            .warn("You are not allowed to add viewers to this scoreboard. This Scoreboard automatically adds viewers.")
    }

    override fun removeViewer(viewer: Player) {
        ComponentLogger.logger()
            .warn("You are not allowed to remove viewers from this scoreboard. This Scoreboard automatically removes viewers.")
    }

    override fun onUpdate() {
        forEachPlayer { addViewerInternal(it) }
    }
}
