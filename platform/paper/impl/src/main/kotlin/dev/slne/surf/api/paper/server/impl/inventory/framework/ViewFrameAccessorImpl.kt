package dev.slne.surf.api.paper.server.impl.inventory.framework

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.paper.inventory.framework.ViewFrameAccessor
import dev.slne.surf.api.paper.server.inventory.framework.InventoryLoader
import me.devnatan.inventoryframework.ViewFrame

@Service
class ViewFrameAccessorImpl : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame {
        return InventoryLoader.viewFrame
    }
}