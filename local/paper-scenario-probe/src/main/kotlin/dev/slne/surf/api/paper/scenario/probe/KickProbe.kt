package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Server
import org.springframework.stereotype.Component

/**
 * `kick <player>`: disconnects the player from the server's side, on the player's thread.
 */
@Component
class KickProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "kick"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())

        withContext(dispatchers.entity(player)) {
            player.kick()
        }

        return "ok"
    }
}
