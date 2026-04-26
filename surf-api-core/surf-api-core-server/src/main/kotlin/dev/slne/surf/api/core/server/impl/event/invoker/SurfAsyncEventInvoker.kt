package dev.slne.surf.api.core.server.impl.event.invoker

import dev.slne.surf.api.core.event.SurfAsyncEvent

fun interface SurfAsyncEventInvoker {

    suspend fun invoke(event: SurfAsyncEvent)
}