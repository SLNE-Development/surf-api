package dev.slne.surf.api.core.server.event

import dev.slne.surf.api.core.event.SurfAsyncEvent
import dev.slne.surf.api.core.event.SurfSyncEvent
import dev.slne.surf.api.core.event.invoker.SurfAsyncEventInvoker
import dev.slne.surf.api.core.event.invoker.SurfAsyncEventInvokerTemplate
import dev.slne.surf.api.core.event.invoker.SurfSyncEventInvoker
import dev.slne.surf.api.core.event.invoker.SurfSyncEventInvokerTemplate
import dev.slne.surf.api.core.invoker.InvokerFactory
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import java.lang.reflect.Method

@InternalInvokerApi
internal object SurfEventInvokerFactory {

    private val syncFactory = InvokerFactory(
        SurfSyncEventInvokerTemplate::class.java,
        SurfSyncEventInvoker::class.java
    )

    private val asyncFactory = InvokerFactory(
        SurfAsyncEventInvokerTemplate::class.java,
        SurfAsyncEventInvoker::class.java
    )

    fun createSync(
        target: Any,
        method: Method,
        eventClass: Class<out SurfSyncEvent>
    ): SurfSyncEventInvoker = syncFactory.create(target, method, eventClass, false)

    fun createAsync(
        target: Any,
        method: Method,
        eventClass: Class<out SurfAsyncEvent>
    ): SurfAsyncEventInvoker = asyncFactory.create(target, method, eventClass)
}
