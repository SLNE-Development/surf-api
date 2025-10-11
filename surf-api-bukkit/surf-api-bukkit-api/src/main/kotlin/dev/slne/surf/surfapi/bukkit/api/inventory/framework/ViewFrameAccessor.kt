package dev.slne.surf.surfapi.bukkit.api.inventory.framework

import dev.slne.surf.surfapi.core.api.util.requiredService
import me.devnatan.inventoryframework.ViewFrame
import org.jetbrains.annotations.ApiStatus

@ApiStatus.NonExtendable
interface ViewFrameAccessor {
    fun viewFrame(): ViewFrame

    companion object {
        val instance = requiredService<ViewFrameAccessor>()
    }
}

val viewFrame: ViewFrame
    get() = ViewFrameAccessor.instance.viewFrame()