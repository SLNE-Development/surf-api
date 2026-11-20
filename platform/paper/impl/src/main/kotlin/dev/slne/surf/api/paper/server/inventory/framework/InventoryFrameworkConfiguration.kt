package dev.slne.surf.api.paper.server.inventory.framework

import dev.slne.surf.api.paper.server.plugin
import dev.slne.surf.api.spring.SurfLifecycle
import me.devnatan.inventoryframework.ViewFrame
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Service

/**
 * Publishes the inventory framework's [ViewFrame] as a bean of surf-api's root context, so that
 * plugins inject it.
 */
@Configuration
internal class InventoryFrameworkConfiguration {

    /**
     * The one [ViewFrame] of the server, owned by surf-api's plugin.
     *
     * [InventoryFrameworkLifecycle] registers and unregisters it; the context must not.
     */
    @Bean(destroyMethod = "")
    fun viewFrame(): ViewFrame = ViewFrame.create(plugin)
}

/**
 * Registers the [ViewFrame] with the server while surf-api is enabled.
 */
@Service
internal class InventoryFrameworkLifecycle(private val viewFrame: ViewFrame) : SurfLifecycle {

    override suspend fun onEnable() {
        viewFrame.register()
    }

    override suspend fun onDisable() {
        viewFrame.unregister()
    }
}
