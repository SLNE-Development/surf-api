package dev.slne.surf.api.paper.server.display

import dev.slne.surf.api.paper.server.display.user.DisplayUser
import org.bukkit.entity.Player
import java.util.*

object DisplayManager {
    private val activeDisplays = mutableMapOf<UUID, Display>()

    fun open(player: Player, display: Display) {
        close(player)
        display.spawn(player)
        activeDisplays[player.uniqueId] = display
    }

    fun close(player: Player) {
        activeDisplays.remove(player.uniqueId)?.despawn(player)
    }

    fun getDisplay(uuid: UUID): Display? = activeDisplays[uuid]

    fun hasDisplay(uuid: UUID): Boolean = activeDisplays.containsKey(uuid)

    fun closeAll() {
        for ((uuid, display) in activeDisplays.toMap()) {
            val user = DisplayUser.of(uuid)
            user.player?.let { display.despawn(it) }
        }
        activeDisplays.clear()
    }
}
