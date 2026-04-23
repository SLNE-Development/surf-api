package dev.slne.surf.api.core.server.impl.event

import dev.slne.surf.api.core.event.invoker.SurfAsyncEventInvoker
import dev.slne.surf.api.core.event.invoker.SurfAsyncEventInvokerTemplate
import dev.slne.surf.api.core.event.invoker.SurfSyncEventInvoker
import dev.slne.surf.api.core.event.invoker.SurfSyncEventInvokerTemplate
import dev.slne.surf.api.core.invoker.InvokerFactory
import dev.slne.surf.api.shared.api.util.InternalInvokerApi

/**
 * Holder for the two [InvokerFactory] instances used by [SurfEventBusImpl].
 *
 * Initialising the factories loads the template `.class` bytes once, after
 * which they can produce per-handler hidden classes very cheaply.
 */
@InternalInvokerApi
internal object SurfEventInvokerFactory {

    val syncFactory: InvokerFactory<SurfSyncEventInvoker> = InvokerFactory(
        SurfSyncEventInvokerTemplate::class.java,
        SurfSyncEventInvoker::class.java,
    )

    val asyncFactory: InvokerFactory<SurfAsyncEventInvoker> = InvokerFactory(
        SurfAsyncEventInvokerTemplate::class.java,
        SurfAsyncEventInvoker::class.java,
    )
}
