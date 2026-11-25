package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.spring.SurfApiBean
import org.bukkit.entity.EntityType
import org.bukkit.inventory.ItemStack

@NmsUseWithCaution
@SurfApiBean
interface SurfPaperNmsNbtBridge {

    fun makeItemStackEntityInvisible(
        itemStack: ItemStack,
        invisibleEntityType: EntityType,
    ): ItemStack
}
