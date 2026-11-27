package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Server
import org.bukkit.inventory.CraftingInventory
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.MenuType
import org.springframework.stereotype.Component

/**
 * `crafting-inputs <player>`: the crafting inputs of the player's open view, read on the player's
 * thread.
 *
 * The inputs are those of an open crafting table, or of the player's own crafting grid when no
 * container is open. Replies with the items as `MATERIAL:amount,...`, sorted, or `none`, also when
 * the open view has no crafting grid.
 */
@Component
class CraftingInputsProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "crafting-inputs"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())
        val inputs = withContext(dispatchers.entity(player)) {
            val crafting = player.openInventory.topInventory as? CraftingInventory
                ?: return@withContext emptyList()

            crafting.matrix
                .filterNotNull()
                .filterNot { it.isEmpty }
                .map { it.describe() }
        }

        return inputs.sorted().ifEmpty { listOf("none") }.joinToString(",")
    }
}

/**
 * `set-crafting-input <player> <index> <material> <amount>`: puts an item into a crafting input of
 * the player's open view, which must have a crafting grid.
 */
@Component
class SetCraftingInputProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "set-crafting-input"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, index, material, amount) = arguments
        val player = server.onlinePlayer(playerName)

        withContext(dispatchers.entity(player)) {
            val crafting = player.openInventory.topInventory as? CraftingInventory
                ?: throw IllegalArgumentException("$playerName has no crafting grid open")
            val matrix = crafting.matrix

            matrix[index.toInt()] = ItemStack(material(material), amount.toInt())
            crafting.matrix = matrix
        }

        return "ok"
    }
}

/**
 * `open-anvil <player> <material> <amount>`: opens an anvil menu at the player's position whose
 * first input holds the item.
 *
 * The menu belongs to a location, so closing it hands its inputs back to the player as an anvil
 * block does.
 */
@Component
class OpenAnvilProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "open-anvil"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, material, amount) = arguments
        val player = server.onlinePlayer(playerName)

        withContext(dispatchers.entity(player)) {
            val anvil = MenuType.ANVIL.builder()
                .location(player.location)
                .checkReachable(false)
                .build(player)

            player.openInventory(anvil)
            anvil.topInventory.setItem(0, ItemStack(material(material), amount.toInt()))
        }

        return "ok"
    }
}

/**
 * `close-view <player>`: closes the player's open view.
 */
@Component
class CloseViewProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "close-view"

    override suspend fun run(arguments: List<String>): String {
        val player = server.onlinePlayer(arguments.single())

        withContext(dispatchers.entity(player)) {
            player.closeInventory()
        }

        return "ok"
    }
}
