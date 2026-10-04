package dev.slne.surf.api.minestom.event

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

/**
 * Contributes listeners to an event node, usually the one of the surf api.
 *
 * ```
 * class WelcomeListener : EventRegistrar {
 *     override fun register(node: EventNode<Event>) {
 *         node.addListener<PlayerSpawnEvent> { it.player.sendMessage("Welcome!") }
 *     }
 * }
 *
 * surfMinestomApi {
 *     listeners(WelcomeListener())
 * }
 * ```
 */
fun interface EventRegistrar {

    fun register(node: EventNode<Event>)
}
