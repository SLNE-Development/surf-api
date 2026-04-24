package dev.slne.surf.api.core.server.impl.event.invoker

import dev.slne.surf.api.core.invoker.InvokerFactory
import dev.slne.surf.api.shared.api.util.InternalInvokerApi

@OptIn(InternalInvokerApi::class)
object SurfEventInvokerFactory {
    val syncFactory: InvokerFactory<SurfSyncEventInvoker> = InvokerFactory(
        SurfSyncEventInvokerTemplate::class.java,
        SurfSyncEventInvoker::class.java
    )

    val asyncFactory: InvokerFactory<SurfAsyncEventInvoker> = InvokerFactory(
        SurfAsyncEventInvokerTemplate::class.java,
        SurfAsyncEventInvoker::class.java,
    )
}