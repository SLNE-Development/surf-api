@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack
import dev.slne.surf.api.spring.SurfApiBean

@NmsUseWithCaution
@SurfApiBean
interface SurfPaperNmsNbtBridge {

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
}
