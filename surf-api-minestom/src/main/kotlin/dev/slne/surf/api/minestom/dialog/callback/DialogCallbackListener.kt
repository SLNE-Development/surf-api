package dev.slne.surf.api.minestom.dialog.callback

import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.event.EventRegistrar
import dev.slne.minestom.lobby.api.extension.addListener
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import net.minestom.server.event.player.PlayerCustomClickEvent

/**
 * Runs the [DialogCallbacks] a client reports back after pressing a dialog button.
 */
@Singleton
class DialogCallbackListener : EventRegistrar {
    override fun register(node: EventNode<Event>) {
        node.addListener<PlayerCustomClickEvent> { event ->
            DialogCallbacks.dispatch(event.player, event.key, event.payload)
        }
    }
}
