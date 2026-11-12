package dev.slne.surf.api.minestom.server.impl.inventory.framework

import dev.slne.surf.api.di.Service
import dev.slne.surf.api.minestom.inventory.framework.ViewFrameAccessor
import me.devnatan.inventoryframework.ViewFrame

@Service
internal class ViewFrameAccessorImpl : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame = MinestomInventoryLoader.instance.viewFrame
}
