package dev.slne.surf.api.minestom.event

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

/**
 * Contributes listeners to the server's own event node.
 *
 * ```
 * class WelcomeListener : EventRegistrar {
 *     override fun register(node: EventNode<Event>) {
 *         node.addListener<PlayerSpawnEvent> { it.player.sendMessage("Welcome!") }
 *     }
 * }
 *
 * // in a module:
 * binder().bindEventRegistrar<WelcomeListener>()
 * ```
 *
 * @see dev.slne.surf.api.minestom.command.CommandRegistrar for the command equivalent
 */
fun interface EventRegistrar {

    fun register(node: EventNode<Event>)
}
