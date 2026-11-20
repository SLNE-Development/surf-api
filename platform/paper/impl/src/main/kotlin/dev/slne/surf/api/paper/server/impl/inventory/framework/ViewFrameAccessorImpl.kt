package dev.slne.surf.api.paper.server.impl.inventory.framework

import dev.slne.surf.api.paper.inventory.framework.ViewFrameAccessor
import me.devnatan.inventoryframework.ViewFrame
import org.springframework.stereotype.Service

@Service
class ViewFrameAccessorImpl(private val viewFrame: ViewFrame) : ViewFrameAccessor {
    override fun viewFrame(): ViewFrame = viewFrame
}
