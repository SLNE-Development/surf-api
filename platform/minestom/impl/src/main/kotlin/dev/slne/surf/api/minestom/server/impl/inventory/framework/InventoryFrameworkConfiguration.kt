package dev.slne.surf.api.minestom.server.impl.inventory.framework

import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.inventory.framework.view.clearViewNavigationHistory
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.spring.SurfLifecycle
import me.devnatan.inventoryframework.ViewFrame
import net.minestom.server.event.player.PlayerDisconnectEvent
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
     * The one [ViewFrame] of the server, listening on surf-api's event node.
     *
     * [InventoryFrameworkLifecycle] registers and unregisters it; the context must not.
     */
    @Bean(destroyMethod = "")
    fun viewFrame(server: SurfMinestomServer): ViewFrame = ViewFrame.create(server.eventNode)
}

/**
 * Registers the [ViewFrame] while surf-api is enabled, and clears a player's view navigation
 * history when the player disconnects.
 */
@Service
internal class InventoryFrameworkLifecycle(
    private val viewFrame: ViewFrame,
    private val server: SurfMinestomServer,
) : SurfLifecycle {

    override suspend fun onLoad() {
        server.eventNode.addListener<PlayerDisconnectEvent> { event ->
            clearViewNavigationHistory(event.player)
        }
    }

    override suspend fun onEnable() {
        viewFrame.register()
    }

    override suspend fun onDisable() {
        viewFrame.unregister()
    }
}
