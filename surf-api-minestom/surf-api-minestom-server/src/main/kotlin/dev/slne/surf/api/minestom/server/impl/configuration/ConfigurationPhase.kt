package dev.slne.surf.api.minestom.server.impl.configuration

import dev.slne.surf.api.minestom.extension.ConnectionManager
import dev.slne.surf.api.minestom.server.configuration.ConfigurationContext
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTask
import dev.slne.surf.api.minestom.server.configuration.ConfigurationTaskId
import dev.slne.surf.api.minestom.server.impl.mixin.duck.ConnectionManagerDuck
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.property.ServerProperties
import org.jetbrains.annotations.Blocking

/**
 * Runs the configuration phase as an ordered list of [ConfigurationTask]s.
 *
 * `ConnectionManagerMixin` hands `ConnectionManager#doConfiguration` over to
 * [ConfigurationPhaseHook] while the phase is installed; otherwise Minestom runs its own
 * configuration.
 */
internal object ConfigurationPhase {
    private val LOGGER = ComponentLogger.logger()

    @Volatile
    private var tasks: List<Pair<ConfigurationTaskId, ConfigurationTask>>? = null

    internal fun install(
        tasks: List<Pair<ConfigurationTaskId, ConfigurationTask>>,
        node: EventNode<Event>,
    ) {
        tasks.forEach { (_, task) -> task.install(node) }
        this.tasks = tasks
    }

    internal fun uninstall() {
        tasks = null
    }

    fun stopKeepAlive(player: Player) {
        ConnectionManager.duck().`surf$keepAlivePlayers`().remove(player)
    }

    /**
     * Runs the configuration of [player] in place of Minestom's, on the virtual thread Minestom
     * started for it.
     *
     * @return `false` when the phase is not installed and Minestom has to run its own
     */
    @Blocking
    internal fun run(manager: ConnectionManagerDuck, player: Player, isFirstConfig: Boolean): Boolean {
        val tasks = tasks ?: return false
        configure(manager, player, isFirstConfig, tasks)
        return true
    }

    @Blocking
    private fun configure(
        manager: ConnectionManagerDuck,
        player: Player,
        isFirstConfig: Boolean,
        tasks: List<Pair<ConfigurationTaskId, ConfigurationTask>>,
    ) {
        check(ServerProperties.INSIDE_TEST.get() || Thread.currentThread().isVirtual) {
            "The configuration must run on a virtual thread"
        }

        if (isFirstConfig) {
            manager.`surf$configurationPlayers`().add(player)
            manager.`surf$keepAlivePlayers`().add(player)

            player.refreshKeepAlive(System.nanoTime())
            player.refreshAnswerKeepAlive(true)
        }

        val context = ConfigurationContext(player, isFirstConfig)

        for ((id, task) in tasks) {
            if (!player.isOnline) return

            try {
                task.run(context)
            } catch (failure: Throwable) {
                LOGGER.error("Configuration step '{}' failed for {}", id, player.username, failure)
                throw failure
            }
        }
    }
}

@Suppress("CAST_NEVER_SUCCEEDS", "NOTHING_TO_INLINE")
private inline fun net.minestom.server.network.ConnectionManager.duck() = this as ConnectionManagerDuck

/** Entry point of `ConnectionManagerMixin`. */
object ConfigurationPhaseHook {

    /**
     * Runs the surf configuration phase for [player] of [manager] when it is installed, on the
     * virtual thread Minestom started for the configuration; a failure propagates to Minestom,
     * which kicks the player.
     *
     * @return whether the phase ran, so that Minestom's own configuration must be skipped
     */
    @Blocking
    @JvmStatic
    fun configure(manager: ConnectionManagerDuck, player: Player, isFirstConfig: Boolean): Boolean =
        ConfigurationPhase.run(manager, player, isFirstConfig)
}
