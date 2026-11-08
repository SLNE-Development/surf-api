package dev.slne.surf.api.paper.inventory.framework

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.inventory.framework.ViewFrameAccessor.Companion.viewFrame
import me.devnatan.inventoryframework.ViewFrame
import org.jetbrains.annotations.ApiStatus

/**
 * Service accessor for the inventory-framework [ViewFrame].
 *
 * Implementations of this interface are loaded via the service locator pattern
 * (`requiredService<ViewFrameAccessor>()`). The single instance is available through
 * [ViewFrameAccessor.INSTANCE] or the top-level [viewFrame] property.
 *
 * This interface is not intended to be implemented outside the framework itself.
 *
 * @see viewFrame
 */
@ApiStatus.NonExtendable
interface ViewFrameAccessor {
    /**
     * Returns the active [ViewFrame] managed by the inventory framework.
     *
     * The [ViewFrame] is the central registry for all registered views and is used
     * to open, close, and manage inventory views.
     *
     * @return the active [ViewFrame]
     */
    fun viewFrame(): ViewFrame

    companion object : ViewFrameAccessor by accessor {
        /**
         * The singleton [ViewFrameAccessor] instance resolved from the service registry.
         */
        val INSTANCE get() = accessor
    }
}

private val accessor = requiredService<ViewFrameAccessor>()

/**
 * Top-level shortcut to the active [ViewFrame].
 *
 * Delegates to [ViewFrameAccessor.INSTANCE] on every access.
 *
 * ```kotlin
 * viewFrame.open(MyView::class.java, player)
 * ```
 *
 * @see ViewFrameAccessor
 */
val viewFrame: ViewFrame get() = ViewFrameAccessor.INSTANCE.viewFrame()