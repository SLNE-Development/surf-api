package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Location
import org.bukkit.Server
import org.bukkit.entity.Item
import org.springframework.stereotype.Component

/**
 * `world`: the name and the unique id of the server's first world, as `<name> <uuid>`.
 *
 * The id is stored with the world, so it tells whether a restarted server loaded the same world.
 */
@Component
class WorldProbe(private val server: Server) : ProbeAction {

    override val name = "world"

    override suspend fun run(arguments: List<String>): String {
        val world = server.worlds.first()

        return "${world.name} ${world.uid}"
    }
}

/**
 * `items <world> <x> <y> <z> <radius>`: the dropped items within the radius around the position,
 * read on the thread that owns it.
 *
 * Replies with the items as `MATERIAL:amount,...`, sorted, or `none`.
 */
@Component
class ItemsProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "items"

    override suspend fun run(arguments: List<String>): String {
        val (worldName, x, y, z, radius) = arguments
        val world = server.getWorld(worldName)
            ?: throw IllegalArgumentException("no world $worldName")
        val center = Location(world, x.toDouble(), y.toDouble(), z.toDouble())

        val items = withContext(dispatchers.region(center)) {
            world.getNearbyEntitiesByType(Item::class.java, center, radius.toDouble())
                .map { it.itemStack.describe() }
        }

        return items.sorted().ifEmpty { listOf("none") }.joinToString(",")
    }
}
