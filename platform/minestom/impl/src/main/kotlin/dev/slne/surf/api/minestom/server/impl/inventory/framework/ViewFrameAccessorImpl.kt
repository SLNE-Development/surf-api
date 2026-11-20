package dev.slne.surf.api.minestom.server.impl.inventory.framework

import dev.slne.surf.api.minestom.inventory.framework.ViewFrameAccessor
import me.devnatan.inventoryframework.ViewFrame
import org.springframework.stereotype.Service

@Service
internal class ViewFrameAccessorImpl(private val viewFrame: ViewFrame) : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame = viewFrame
}
