package dev.slne.surf.api.minestom.server.configuration

import net.minestom.server.entity.Player
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.instance.Instance
import org.jetbrains.annotations.Blocking

/**
 * One step of the configuration phase.
 *
 * Steps run in order on a virtual thread, so a step may block until the client has answered it.
 *
 * @see ConfigurationPhaseBuilder
 */
fun interface ConfigurationTask {

    /** Runs the step and blocks until the client has answered it. */
    @Blocking
    fun run(context: ConfigurationContext)

    /**
     * Registers whatever listeners the step needs, once, when the configuration phase is
     * installed.
     */
    fun install(node: EventNode<Event>) = Unit
}

/**
 * Identifies a step of the configuration phase, so that it can be replaced, removed or used as an
 * anchor for further steps.
 *
 * @see ConfigurationTasks for the ids of the default steps
 */
@JvmInline
value class ConfigurationTaskId(val value: String) {
    override fun toString(): String = value
}

/**
 * Everything the steps of one configuration run share.
 */
class ConfigurationContext internal constructor(
    val player: Player,

    /** Whether the player is configured for the first time, rather than sent back from play. */
    val isFirstConfig: Boolean,
) {
    private var configurationEvent: AsyncPlayerConfigurationEvent? = null

    /**
     * The configuration event, available once the [ConfigurationTasks.CONFIGURATION_EVENT] step
     * has run.
     */
    var event: AsyncPlayerConfigurationEvent
        get() = checkNotNull(configurationEvent) {
            "The configuration event has not been called yet; " +
                    "this step has to run after ${ConfigurationTasks.CONFIGURATION_EVENT}"
        }
        set(value) {
            configurationEvent = value
        }

    /** The instance the player will spawn in, as chosen in the configuration [event]. */
    val spawningInstance: Instance
        get() = requireNotNull(event.spawningInstance) {
            "You need to specify a spawning instance in the AsyncPlayerConfigurationEvent"
        }
}
