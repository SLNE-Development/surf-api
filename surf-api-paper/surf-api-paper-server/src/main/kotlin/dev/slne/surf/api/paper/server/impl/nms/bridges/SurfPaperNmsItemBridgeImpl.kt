package dev.slne.surf.api.paper.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsItemBridge
import dev.slne.surf.api.paper.server.nms.nms
import net.minecraft.core.component.DataComponentMap
import net.minecraft.core.component.DataComponents
import org.bukkit.inventory.ItemType

@AutoService(SurfPaperNmsItemBridge::class)
@NmsUseWithCaution
class SurfPaperNmsItemBridgeImpl : SurfPaperNmsItemBridge {
    init {
        checkInstantiationByServiceLoader()
    }

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