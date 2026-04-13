package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.nms
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemType

@NmsUseWithCaution
class V26_1SurfPaperNmsItemBridgeImpl : SurfPaperNmsItemBridge {

    override fun setDefaultMaxStackSize(item: ItemType, maxStackSize: Int) {
        require(maxStackSize in 1..100) { "Max stack size must be between 1 and 100" }

        val nmsItem = item.nms
        val updatedComponents = DataComponentMap.builder()
            .addAll(nmsItem.components())
            .set(DataComponents.MAX_STACK_SIZE, maxStackSize)
            .build()

        nmsItem.builtInRegistryHolder().bindComponents(updatedComponents)
    }
}
