package dev.slne.surf.api.minestom.impl.inventory.framework

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import dev.slne.surf.api.minestom.inventory.framework.view.ViewNavigationHistory
import me.devnatan.inventoryframework.ViewFrame
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent

/**
 * Loads, enables and disables the inventory-framework [ViewFrame] for Minestom.
 */
@Singleton
class MinestomInventoryLoader : EventRegistrar {
    companion object {
        lateinit var instance: MinestomInventoryLoader
            private set
    }

    lateinit var viewFrame: ViewFrame
        private set

    private var registered = false

    init {
        instance = this
    }

    override fun register(node: EventNode<Event>) {
        viewFrame = ViewFrame.create(node)

        node.addListener<PlayerDisconnectEvent> { event ->
            ViewNavigationHistory.clear(event.player)
        }
    }

    fun enable() {
        if (registered) return
        registered = true

        viewFrame.register()
    }

    fun disable() {
        if (!registered || !::viewFrame.isInitialized) return
        registered = false
        viewFrame.unregister()
    }
}
