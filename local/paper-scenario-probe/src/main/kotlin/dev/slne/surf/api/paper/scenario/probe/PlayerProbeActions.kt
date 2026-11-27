package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Material
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Component

/**
 * `inventory <player>`: the player's inventory as the server sees it, read on the player's thread.
 *
 * Replies `held=<hotbar slot> cursor=<item> view=<open inventory type> slots=<slot:item,...>`, with
 * items as `MATERIAL:amount`, the cursor as `AIR` when empty, `view=CRAFTING` when no container is
 * open, and `slots=none` for an empty inventory.
 */
@Component
class InventoryProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "inventory"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())

        return withContext(dispatchers.entity(player)) { describe(player) }
    }

    private fun describe(player: Player): String {
        val inventory = player.inventory
        val slots = (0 until inventory.size)
            .mapNotNull { slot ->
                inventory.getItem(slot)
                    ?.takeUnless { it.isEmpty }
                    ?.let { "$slot:${it.describe()}" }
            }
            .ifEmpty { listOf("none") }
            .joinToString(",")
        val view = player.openInventory.topInventory.type

        return "held=${inventory.heldItemSlot} cursor=${player.itemOnCursor.describe()} " +
            "view=$view slots=$slots"
    }
}

/**
 * `give <player> <slot> <material> <amount>`: puts an item into a slot of the player's inventory.
 */
@Component
class GiveProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "give"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, slot, material, amount) = arguments
        val player = server.onlinePlayer(playerName)
        val item = ItemStack(material(material), amount.toInt())

        withContext(dispatchers.entity(player)) {
            player.inventory.setItem(slot.toInt(), item)
        }

        return "ok"
    }
}

/**
 * `position <player>`: the block position of the player's feet, as `<x> <y> <z>`.
 */
@Component
class PositionProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "position"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())
        val location = withContext(dispatchers.entity(player)) { player.location }

        return "${location.blockX} ${location.blockY} ${location.blockZ}"
    }
}

/**
 * `place <player> <material>`: sets the block two blocks east of the player's feet, within reach,
 * and replies with its position as `<x> <y> <z>`.
 */
@Component
class PlaceProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "place"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, material) = arguments
        val player = server.onlinePlayer(playerName)
        val location = withContext(dispatchers.entity(player)) {
            player.location.add(EAST_OFFSET, 0.0, 0.0)
        }

        withContext(dispatchers.region(location)) {
            location.block.type = material(material)
        }

        return "${location.blockX} ${location.blockY} ${location.blockZ}"
    }

    private companion object {
        const val EAST_OFFSET = 2.0
    }
}

/**
 * `kill <player>`: sets the player's health to zero, so the player dies as from any damage.
 */
@Component
class KillProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "kill"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())

        withContext(dispatchers.entity(player)) {
            player.health = 0.0
        }

        return "ok"
    }
}

/**
 * The online player called [name], or an exception that becomes the request's error.
 */
internal fun Server.onlinePlayer(name: String): Player {
    return getPlayerExact(name) ?: throw IllegalArgumentException("$name is not online")
}

internal fun material(name: String): Material {
    return Material.matchMaterial(name) ?: throw IllegalArgumentException("no material $name")
}

/**
 * The item as `MATERIAL:amount`, or `AIR` when the stack is empty.
 */
internal fun ItemStack.describe(): String {
    return if (isEmpty) "AIR" else "${type.name}:$amount"
}
