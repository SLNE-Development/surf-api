@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.minestom.inventory.framework

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.surfBean
import me.devnatan.inventoryframework.ViewFrame
import org.jetbrains.annotations.ApiStatus
import dev.slne.surf.api.spring.SurfApiBean

/**
 * Provides the server's inventory framework [ViewFrame].
 *
 * The [ViewFrame] is also a Spring bean of surf-api's root context, so consumers can inject it
 * directly instead of this accessor. Only surf-api implements this interface.
 */
@ApiStatus.NonExtendable
@SurfApiBean
interface ViewFrameAccessor {

    /**
     * Returns the server's [ViewFrame], the registry of all inventory views.
     */
    fun viewFrame(): ViewFrame
}

/**
 * The server's [ViewFrame], for the extension functions of this package.
 */
internal val viewFrame: ViewFrame by surfBean<ViewFrame>()
