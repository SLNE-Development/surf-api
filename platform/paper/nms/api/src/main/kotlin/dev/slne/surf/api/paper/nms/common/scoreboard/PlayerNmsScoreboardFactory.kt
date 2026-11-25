package dev.slne.surf.api.paper.nms.common.scoreboard

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import org.bukkit.entity.Player

/**
 * Creates the packet-based sidebar of a player for the selected NMS version.
 */
fun interface PlayerNmsScoreboardFactory {

    /**
     * Creates a sidebar for [player]; nothing is sent before its first update.
     */
    fun create(player: Player): PlayerNmsScoreboard

    companion object {
        @InternalSurfApi
        val instance by surfBean<PlayerNmsScoreboardFactory>()
    }
}
