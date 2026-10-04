package dev.slne.surf.api.minestom.server.configuration

import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import net.minestom.server.entity.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Configures the steps the configuration phase runs, in order.
 *
 * Starts out with the steps vanilla runs, see [ConfigurationTasks]. Each of them can be replaced,
 * removed or surrounded by steps of your own:
 *
 * ```
 * withConfigurationPhase {
 *     codeOfConduct { player -> texts[player.locale] }
 *     replace(ConfigurationTasks.RESOURCE_PACK) { context -> myPacks.await(context.player) }
 *     after(ConfigurationTasks.AWAIT_SETTINGS, ConfigurationTaskId("load_profile")) { context ->
 *         profiles.load(context.player.uuid)
 *     }
 * }
 * ```
 */
@SurfMinestomServerDsl
class ConfigurationPhaseBuilder internal constructor() {
    private val tasks = linkedMapOf(
        ConfigurationTasks.PLAYER_COUNT to ConfigurationTasks.playerCount,
        ConfigurationTasks.BRAND to ConfigurationTasks.brand,
        ConfigurationTasks.CONFIGURATION_EVENT to ConfigurationTasks.configurationEvent,
        ConfigurationTasks.SPAWN_LOCATION to ConfigurationTasks.spawnLocation,
        ConfigurationTasks.ENABLED_FEATURES to ConfigurationTasks.enabledFeatures,
        ConfigurationTasks.SYNCHRONIZE_REGISTRIES to ConfigurationTasks.synchronizeRegistries,
        ConfigurationTasks.AWAIT_SETTINGS to ConfigurationTasks.AwaitSettings(),
        ConfigurationTasks.RESOURCE_PACK to ConfigurationTasks.resourcePack,
        ConfigurationTasks.JOIN_WORLD to ConfigurationTasks.joinWorld,
    )

    /** The ids of the configured steps, in the order they run. */
    val ids: List<ConfigurationTaskId> get() = tasks.keys.toList()

    /** Replaces the step [id] with [task], keeping its position. */
    fun replace(id: ConfigurationTaskId, task: ConfigurationTask) {
        require(id in tasks) { "There is no configuration step '$id' to replace" }
        tasks[id] = task
    }

    /** Removes the step [id]. */
    fun remove(id: ConfigurationTaskId) {
        require(tasks.remove(id) != null) { "There is no configuration step '$id' to remove" }
    }

    /** Runs [task] as [id] right before the step [anchor]. */
    fun before(anchor: ConfigurationTaskId, id: ConfigurationTaskId, task: ConfigurationTask) =
        insert(anchor, id, task, offset = 0)

    /** Runs [task] as [id] right after the step [anchor]. */
    fun after(anchor: ConfigurationTaskId, id: ConfigurationTaskId, task: ConfigurationTask) =
        insert(anchor, id, task, offset = 1)

    /** Runs [task] as [id] before every other step. */
    fun first(id: ConfigurationTaskId, task: ConfigurationTask) {
        requireUnknown(id)
        val entries = tasks.entries.map { it.toPair() }
        tasks.clear()
        tasks[id] = task
        tasks.putAll(entries)
    }

    /**
     * Runs [task] as [id] after every other step so far.
     *
     * Mind that [ConfigurationTasks.JOIN_WORLD] hands the player over to the play phase, so a step
     * after it no longer runs during the configuration.
     */
    fun last(id: ConfigurationTaskId, task: ConfigurationTask) {
        requireUnknown(id)
        tasks[id] = task
    }

    /** Waits up to [timeout] for the client's settings. */
    fun awaitSettings(timeout: Duration = 2.seconds) {
        replace(ConfigurationTasks.AWAIT_SETTINGS, ConfigurationTasks.AwaitSettings(timeout))
    }

    /**
     * Shows the code of conduct [text] returns for a player and holds the configuration until the
     * player has accepted it. A player for whom [text] returns `null` is let through.
     *
     * The step runs after the settings arrived, so that [Player.getLocale] is the client's own.
     */
    fun codeOfConduct(text: (Player) -> String?) {
        val task = ConfigurationTasks.CodeOfConduct(text)
        if (ConfigurationTasks.CODE_OF_CONDUCT in tasks) {
            replace(ConfigurationTasks.CODE_OF_CONDUCT, task)
            return
        }

        val anchor = when {
            ConfigurationTasks.AWAIT_SETTINGS in tasks -> ConfigurationTasks.AWAIT_SETTINGS
            ConfigurationTasks.SYNCHRONIZE_REGISTRIES in tasks -> ConfigurationTasks.SYNCHRONIZE_REGISTRIES
            else -> null
        }

        if (anchor != null) {
            after(anchor, ConfigurationTasks.CODE_OF_CONDUCT, task)
        } else {
            first(ConfigurationTasks.CODE_OF_CONDUCT, task)
        }
    }

    private fun insert(
        anchor: ConfigurationTaskId,
        id: ConfigurationTaskId,
        task: ConfigurationTask,
        offset: Int,
    ) {
        requireUnknown(id)
        val entries = tasks.entries.map { it.toPair() }.toMutableList()
        val index = entries.indexOfFirst { it.first == anchor }
        require(index != -1) { "There is no configuration step '$anchor'" }

        entries.add(index + offset, id to task)
        tasks.clear()
        tasks.putAll(entries)
    }

    private fun requireUnknown(id: ConfigurationTaskId) {
        require(id !in tasks) { "The configuration step '$id' already exists, use replace instead" }
    }

    internal fun build(): List<Pair<ConfigurationTaskId, ConfigurationTask>> =
        tasks.entries.map { it.toPair() }
}
