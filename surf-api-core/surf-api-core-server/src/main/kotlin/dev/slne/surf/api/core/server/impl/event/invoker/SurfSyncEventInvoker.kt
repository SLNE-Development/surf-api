package dev.slne.surf.api.core.server.impl.event.invoker

import dev.slne.surf.api.core.event.SurfSyncEvent

fun interface SurfSyncEventInvoker {

    fun invoke(event: SurfSyncEvent)
}