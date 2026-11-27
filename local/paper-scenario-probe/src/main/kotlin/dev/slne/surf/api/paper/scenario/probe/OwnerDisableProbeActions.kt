package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import dev.slne.surf.api.paper.inventory.temporary.TemporaryInventoryEndEvent
import dev.slne.surf.api.spring.SurfLifecycle
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

/**
 * Calls `endAsync()` in the probe's disable phase for every session the probe began that already
 * ended, as an owning plugin may, and logs each result as the event
 * `temp-disable-end <player> <result>`.
 *
 * At shutdown the probe's sessions end before its disable phase, so the hook meets them ended.
 */
@Component
class TemporaryInventoryDisableHook(
    private val state: TemporaryInventoryProbeState,
) : SurfLifecycle {

    private val log = logger()

    override suspend fun onDisable() {
        for ((playerName, session) in state.sessions) {
            if (session.isActive) continue

            val ended = session.endAsync()

            log.atInfo()
                .log("%s %s %s %s", ProbeProtocol.EVENT, DISABLE_END_EVENT, playerName, ended)
        }
    }

    companion object {
        const val DISABLE_END_EVENT = "temp-disable-end"
    }
}

private fun Server.installedPlugin(name: String): Plugin {
    return pluginManager.getPlugin(name) ?: throw IllegalArgumentException("no plugin $name")
}
