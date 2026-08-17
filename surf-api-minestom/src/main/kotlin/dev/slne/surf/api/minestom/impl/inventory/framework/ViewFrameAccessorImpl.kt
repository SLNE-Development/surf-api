package dev.slne.surf.api.minestom.impl.inventory.framework

import com.google.auto.service.AutoService
import dev.slne.surf.api.minestom.inventory.framework.ViewFrameAccessor
import me.devnatan.inventoryframework.ViewFrame

@AutoService(ViewFrameAccessor::class)
internal class ViewFrameAccessorImpl : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame = MinestomInventoryLoader.instance.viewFrame
}
