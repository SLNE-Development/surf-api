package dev.slne.surf.api.core.server.impl.event.invoker

import dev.slne.surf.api.core.invoker.InvokerFactory
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import java.lang.invoke.MethodHandles

@OptIn(InternalInvokerApi::class)
object SurfEventInvokerFactory {
    val lookup: MethodHandles.Lookup = MethodHandles.lookup()

    val syncFactory: InvokerFactory<SurfSyncEventInvoker> = InvokerFactory(
        SurfSyncEventInvokerTemplate::class.java,
        SurfSyncEventInvoker::class.java,
        lookup
    )

    val asyncFactory: InvokerFactory<SurfAsyncEventInvoker> = InvokerFactory(
        SurfAsyncEventInvokerTemplate::class.java,
        SurfAsyncEventInvoker::class.java,
        lookup
    )
}