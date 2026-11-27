package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndEvent
import kotlinx.coroutines.withContext
import org.bukkit.Server
import org.bukkit.plugin.Plugin
import org.springframework.stereotype.Component

/**
 * `disable-plugin <name>`: disables the plugin named `name` while the server keeps running.
 */
@Component
class DisablePluginProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "disable-plugin"

    override suspend fun run(arguments: List<String>): String {
        val plugin = server.installedPlugin(arguments.single())

        withContext(dispatchers.main) {
            server.pluginManager.disablePlugin(plugin)
        }

        return "ok"
    }
}

/**
 * Logs where every [TemporaryInventoryEndEvent] fired as the event
 * `temp-end-context <player> <cause> <session id> <owns player> <probe enabled>`.
 *
 * `owns player` tells whether the firing thread owns the player; `probe enabled` whether the probe
 * was still enabled.
 */
@Component
class TemporaryInventoryEndContextLog(
    private val server: Server,
    private val plugin: Plugin,
) {

    private val log = logger()

    @SurfEventHandler
    fun onEnd(event: TemporaryInventoryEndEvent) {
        log.atInfo()
            .log(
                "%s %s %s %s %s %s %s",
                ProbeProtocol.EVENT,
                END_CONTEXT_EVENT,
                event.player.name,
                event.cause,
                event.sessionId,
                server.isOwnedByCurrentRegion(event.player),
                plugin.isEnabled,
            )
    }

    companion object {
        const val END_CONTEXT_EVENT = "temp-end-context"
    }
}

private fun Server.installedPlugin(name: String): Plugin {
    return pluginManager.getPlugin(name) ?: throw IllegalArgumentException("no plugin $name")
}
