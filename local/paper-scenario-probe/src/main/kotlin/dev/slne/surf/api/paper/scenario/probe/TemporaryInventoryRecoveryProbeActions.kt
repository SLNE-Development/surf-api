package dev.slne.surf.api.paper.scenario.probe

import dev.slne.surf.api.paper.coroutine.PaperDispatchers
import io.papermc.paper.registry.RegistryAccess
import io.papermc.paper.registry.RegistryKey
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import org.bukkit.Server
import org.bukkit.inventory.ItemStack
import org.springframework.stereotype.Component

/**
 * `temp-begin-owned <player> <owner> <key> <items>`: begins a temporary inventory session as
 * `temp-begin` does, owned by the installed plugin called `owner` instead of the probe.
 *
 * A test removes the owner afterwards, while the probe stays to observe the recovery.
 */
@Component
class TemporaryInventoryOwnedBeginProbe(
    private val server: Server,
    private val beginner: TemporaryInventoryBeginner,
) : ProbeAction {

    override val name = "temp-begin-owned"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, ownerName, key, items) = arguments
        val owner = server.pluginManager.getPlugin(ownerName)
            ?: throw IllegalArgumentException("no plugin $ownerName")

        return beginner.begin(server.onlinePlayer(playerName), key, parseItems(items), owner)
    }
}

/**
 * `give-enchanted <player> <slot> <material> <enchantment>`: puts one item of the material,
 * enchanted at level 1 with the enchantment of that key, into a slot of the player's inventory.
 *
 * The enchantment may come from a data pack, so the item becomes unreadable once the data pack is
 * gone.
 */
@Component
class GiveEnchantedProbe(
    private val server: Server,
    private val dispatchers: PaperDispatchers,
) : ProbeAction {

    override val name = "give-enchanted"

    override suspend fun run(arguments: List<String>): String {
        val (playerName, slot, material, enchantmentKey) = arguments
        val player = server.onlinePlayer(playerName)
        val enchantment = RegistryAccess.registryAccess()
            .getRegistry(RegistryKey.ENCHANTMENT)
            .get(Key.key(enchantmentKey))
            ?: throw IllegalArgumentException("no enchantment $enchantmentKey")
        val item = ItemStack(material(material))

        item.addUnsafeEnchantment(enchantment, 1)

        withContext(dispatchers.entity(player)) {
            player.inventory.setItem(slot.toInt(), item)
        }

        return "ok"
    }
}
