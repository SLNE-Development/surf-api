package dev.slne.surf.api.minestom.server.impl.inventory.framework

import dev.slne.surf.api.minestom.event.EventRegistrar
import dev.slne.surf.api.minestom.extension.addListener
import dev.slne.surf.api.minestom.inventory.framework.view.clearViewNavigationHistory
import me.devnatan.inventoryframework.ViewFrame
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerDisconnectEvent

/**
 * Loads, enables and disables the inventory-framework [ViewFrame] for Minestom.
 */
internal class MinestomInventoryLoader : EventRegistrar {
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
            clearViewNavigationHistory(event.player)
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
