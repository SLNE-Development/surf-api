package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import kotlinx.coroutines.withContext
import org.bukkit.Server
import org.bukkit.inventory.CraftingInventory
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
