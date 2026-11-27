package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.common.util.logger
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.entity.PlayerDeathEvent
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

/**
 * `death-override <player> <cancel|drop>`: makes the player's next death event, at
 * [EventPriority.HIGHEST] and after surf-api's own handler, either cancelled or no longer keep the
 * inventory, as another plugin may. Logs the event `death-override <player> <mode>` when it acts.
 */
@Component
class DeathOverrideProbe : ProbeAction, Listener {

    private val log = logger()
    private val overrides = ConcurrentHashMap<String, String>()

    override val name = DEATH_OVERRIDE

    override suspend fun run(arguments: List<String>): String {
        val (playerName, mode) = arguments
        require(mode == CANCEL || mode == DROP) { "Unknown death override $mode" }

        overrides[playerName] = mode

        return "ok"
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    fun onDeath(event: PlayerDeathEvent) {
        val mode = overrides.remove(event.player.name) ?: return

        if (mode == CANCEL) {
            event.isCancelled = true
        } else {
            event.keepInventory = false
        }

        log.atInfo()
            .log("%s %s %s %s", ProbeProtocol.EVENT, DEATH_OVERRIDE, event.player.name, mode)
    }

    companion object {
        const val DEATH_OVERRIDE = "death-override"
        private const val CANCEL = "cancel"
        private const val DROP = "drop"
    }
}
