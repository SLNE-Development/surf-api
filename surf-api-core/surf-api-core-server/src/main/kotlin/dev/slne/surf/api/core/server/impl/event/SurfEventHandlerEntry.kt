package dev.slne.surf.api.core.server.impl.event

import dev.slne.surf.api.core.event.SurfEventPriority
import dev.slne.surf.api.core.server.impl.event.invoker.SurfAsyncEventInvoker
import dev.slne.surf.api.core.server.impl.event.invoker.SurfSyncEventInvoker

sealed interface SurfEventHandlerEntry {
    val priority: SurfEventPriority
    val ignoreCancelled: Boolean
    val token: Any

    class SyncInvoker(
        override val token: Any,
        val invoker: SurfSyncEventInvoker,
        override val priority: SurfEventPriority,
        override val ignoreCancelled: Boolean
    ) : SurfEventHandlerEntry

    class AsyncInvoker(
        override val token: Any,
        val invoker: SurfAsyncEventInvoker,
        override val priority: SurfEventPriority,
        override val ignoreCancelled: Boolean
    ) : SurfEventHandlerEntry
}