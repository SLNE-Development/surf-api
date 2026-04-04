package dev.slne.surf.api.paper.server.impl.inventory.framework

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.inventory.framework.ViewFrameAccessor
import dev.slne.surf.api.paper.server.inventory.framework.InventoryLoader
import me.devnatan.inventoryframework.ViewFrame

@AutoService(ViewFrameAccessor::class)
class ViewFrameAccessorImpl : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame {
        return InventoryLoader.viewFrame
    }
}