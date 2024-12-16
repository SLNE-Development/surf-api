package dev.slne.surf.surfapi.bukkit.api.nms.bridges

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.core.api.util.requiredService
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
interface SurfBukkitNmsNbtBridge {

    fun makeItemStackEntityInvisible(
        itemStack: ItemStack,
        invisibleEntityType: EntityType
    ): ItemStack

    /**
     * Get the NBT string from the item stack with the given key
     *
     * @param itemStack the item stack
     * @param key       the key
     * @return the NBT string
     */
    @Deprecated("Now only uses the nbt in the custom data components (item stacks now use data components, and nbt should not be avoided)")
    fun getNbtString(itemStack: ItemStack, key: String): String

    companion object {
        val instance = requiredService<SurfBukkitNmsNbtBridge>()
    }
}

@NmsUseWithCaution
val nmsNbtBridge get() = SurfBukkitNmsNbtBridge.instance
