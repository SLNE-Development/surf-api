package dev.slne.surf.api.minestom.extension

import net.minestom.server.event.Event
import net.minestom.server.event.EventListener
import net.minestom.server.event.EventNode

inline fun <reified T : Event> EventNode<Event>.addListener(
    noinline listener: (T) -> Unit
) {
    addListener(EventListener.of(T::class.java, listener))
}