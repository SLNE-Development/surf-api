package dev.slne.surf.surfapi.bukkit.server.impl.inventory.framework

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.ViewFrameAccessor
import dev.slne.surf.surfapi.bukkit.server.inventory.framework.InventoryLoader
import me.devnatan.inventoryframework.ViewFrame

@AutoService(ViewFrameAccessor::class)
class ViewFrameAccessorImpl: ViewFrameAccessor {
    override fun viewFrame(): ViewFrame {
        return InventoryLoader.viewFrame
    }
}