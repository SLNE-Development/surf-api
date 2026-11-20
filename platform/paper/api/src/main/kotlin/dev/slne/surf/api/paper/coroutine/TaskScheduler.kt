package dev.slne.surf.api.paper.coroutine

import io.papermc.paper.ServerBuildInfo
import net.kyori.adventure.key.Key
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin

/**
 * The part of Paper's schedulers that [PluginDispatchers] uses, separated so that the dispatch
 * protocol can be tested without a server.
 *
 * The `run*` functions may throw when Paper refuses the task, for example with an
 * `IllegalPluginAccessException` once the plugin is disabled.
 */
internal interface TaskScheduler {

    /**
     * Whether the server ticks regions on separate threads, as Folia does.
     */
    val isRegionized: Boolean

    fun isGlobalTickThread(): Boolean

    fun isOwnedByCurrentRegion(world: World, chunkX: Int, chunkZ: Int): Boolean

    fun isOwnedByCurrentRegion(entity: Entity): Boolean

    /**
     * Whether [entity] is still in a world, or is a connected player.
     */
    fun isAlive(entity: Entity): Boolean

    fun runGlobal(task: Runnable)

    fun runRegion(world: World, chunkX: Int, chunkZ: Int, task: Runnable)

    /**
     * Schedules [task] on the thread that owns [entity], or [retired] once the entity is removed
     * first.
     *
     * @return `false` if the entity is already retired; then neither callback runs
     */
    fun runEntity(entity: Entity, task: Runnable, retired: Runnable): Boolean
}

/**
 * [TaskScheduler] on Paper's region-aware schedulers, scheduling on behalf of [plugin].
 */
internal class BukkitTaskScheduler(private val plugin: Plugin) : TaskScheduler {

    override val isRegionized: Boolean = REGIONIZED

    override fun isGlobalTickThread(): Boolean = Bukkit.isGlobalTickThread()

    override fun isOwnedByCurrentRegion(world: World, chunkX: Int, chunkZ: Int): Boolean {
        return Bukkit.isOwnedByCurrentRegion(world, chunkX, chunkZ)
    }

    override fun isOwnedByCurrentRegion(entity: Entity): Boolean {
        return Bukkit.isOwnedByCurrentRegion(entity)
    }

    override fun isAlive(entity: Entity): Boolean {
        return entity.isValid || (entity is Player && entity.isConnected)
    }

    override fun runGlobal(task: Runnable) {
        Bukkit.getGlobalRegionScheduler().run(plugin) { task.run() }
    }

    override fun runRegion(world: World, chunkX: Int, chunkZ: Int, task: Runnable) {
        Bukkit.getRegionScheduler().run(plugin, world, chunkX, chunkZ) { task.run() }
    }

    override fun runEntity(entity: Entity, task: Runnable, retired: Runnable): Boolean {
        return entity.scheduler.run(plugin, { task.run() }, retired) != null
    }

    private companion object {
        private val FOLIA_BRAND_ID = Key.key("papermc", "folia")
        val REGIONIZED = ServerBuildInfo.buildInfo().isBrandCompatible(FOLIA_BRAND_ID)
    }
}
