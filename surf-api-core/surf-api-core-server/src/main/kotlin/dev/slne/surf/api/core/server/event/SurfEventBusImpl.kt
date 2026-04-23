@file:OptIn(dev.slne.surf.api.shared.api.util.InternalInvokerApi::class)
package dev.slne.surf.api.core.server.event

import com.google.auto.service.AutoService
import com.google.common.flogger.FluentLogger
import dev.slne.surf.api.core.event.SurfAsyncEvent
import dev.slne.surf.api.core.event.SurfCancellableEvent
import dev.slne.surf.api.core.event.SurfEvent
import dev.slne.surf.api.core.event.SurfEventBus
import dev.slne.surf.api.core.event.SurfEventHandler
import dev.slne.surf.api.core.event.SurfEventPriority
import dev.slne.surf.api.core.event.SurfSyncEvent
import dev.slne.surf.api.core.event.invoker.SurfAsyncEventInvoker
import dev.slne.surf.api.core.event.invoker.SurfSyncEventInvoker
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

private val log = FluentLogger.forEnclosingClass()

@AutoService(SurfEventBus::class)
class SurfEventBusImpl : SurfEventBus {

    init {
        checkInstantiationByServiceLoader()
    }

    // ---------------------------------------------------------------------------
    // Internal handler entry model
    // ---------------------------------------------------------------------------

    private sealed interface HandlerEntry {
        val priority: SurfEventPriority
        val ignoreCancelled: Boolean
        val token: Any

        class SyncInvoker(
            override val token: Any,
            val invoker: SurfSyncEventInvoker,
            override val priority: SurfEventPriority,
            override val ignoreCancelled: Boolean
        ) : HandlerEntry

        class AsyncInvoker(
            override val token: Any,
            val invoker: SurfAsyncEventInvoker,
            override val priority: SurfEventPriority,
            override val ignoreCancelled: Boolean
        ) : HandlerEntry

        class SyncLambda(
            override val token: Any,
            val handler: (SurfSyncEvent) -> Unit,
            override val priority: SurfEventPriority,
            override val ignoreCancelled: Boolean
        ) : HandlerEntry

        class AsyncLambda(
            override val token: Any,
            val handler: suspend (SurfAsyncEvent) -> Unit,
            override val priority: SurfEventPriority,
            override val ignoreCancelled: Boolean
        ) : HandlerEntry
    }

    /** EventClass → Priority → handlers */
    private val handlers =
        ConcurrentHashMap<Class<out SurfEvent>, ConcurrentHashMap<SurfEventPriority, CopyOnWriteArrayList<HandlerEntry>>>()

    /** token → all (eventClass, entry) pairs registered for cleanup */
    private val tokenMap =
        ConcurrentHashMap<Any, CopyOnWriteArrayList<Pair<Class<out SurfEvent>, HandlerEntry>>>()

    // ---------------------------------------------------------------------------
    // registerListeners (annotation-based)
    // ---------------------------------------------------------------------------

    @OptIn(InternalInvokerApi::class)
    override fun registerListeners(listener: Any) {
        val clazz = listener.javaClass
        val allMethods = (clazz.methods.toList() + clazz.declaredMethods.toList())
            .distinctBy { "${it.name}(${it.parameterTypes.joinToString { p -> p.name }})" }

        for (method in allMethods) {
            val annotation = method.getAnnotation(SurfEventHandler::class.java) ?: continue
            method.isAccessible = true

            val paramTypes = method.parameterTypes
            val isSuspend = HiddenInvokerUtil.isSuspendFunction(method)
            val eventParamCount = if (isSuspend) paramTypes.size - 1 else paramTypes.size

            if (eventParamCount != 1) {
                throw IllegalArgumentException(
                    "Handler method ${clazz.name}#${method.name} must have exactly 1 parameter " +
                        "(the event type), but has $eventParamCount event parameter(s)"
                )
            }

            val eventType = paramTypes[0]

            when {
                SurfSyncEvent::class.java.isAssignableFrom(eventType) -> {
                    if (isSuspend) {
                        throw IllegalArgumentException(
                            "Handler method ${clazz.name}#${method.name} handles a SurfSyncEvent " +
                                "but is declared as suspend. Sync event handlers must not be suspend."
                        )
                    }
                    @Suppress("UNCHECKED_CAST")
                    val syncEventClass = eventType as Class<out SurfSyncEvent>
                    val invoker = SurfEventInvokerFactory.createSync(listener, method, syncEventClass)
                    addEntry(
                        syncEventClass,
                        listener,
                        HandlerEntry.SyncInvoker(listener, invoker, annotation.priority, annotation.ignoreCancelled)
                    )
                }

                SurfAsyncEvent::class.java.isAssignableFrom(eventType) -> {
                    @Suppress("UNCHECKED_CAST")
                    val asyncEventClass = eventType as Class<out SurfAsyncEvent>
                    val invoker = SurfEventInvokerFactory.createAsync(listener, method, asyncEventClass)
                    addEntry(
                        asyncEventClass,
                        listener,
                        HandlerEntry.AsyncInvoker(listener, invoker, annotation.priority, annotation.ignoreCancelled)
                    )
                }

                else -> log.atWarning().log(
                    "Handler method %s#%s has parameter type %s which is neither SurfSyncEvent " +
                        "nor SurfAsyncEvent — skipping",
                    clazz.name, method.name, eventType.name
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // registerHandler / registerAsyncHandler (lambda-based)
    // ---------------------------------------------------------------------------

    override fun <T : SurfSyncEvent> registerHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit
    ): Any {
        val token = Any()
        @Suppress("UNCHECKED_CAST")
        val entry = HandlerEntry.SyncLambda(
            token,
            handler as (SurfSyncEvent) -> Unit,
            priority,
            ignoreCancelled
        )
        addEntry(eventClass, token, entry)
        return token
    }

    override fun <T : SurfAsyncEvent> registerAsyncHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: suspend (T) -> Unit
    ): Any {
        val token = Any()
        @Suppress("UNCHECKED_CAST")
        val entry = HandlerEntry.AsyncLambda(
            token,
            handler as suspend (SurfAsyncEvent) -> Unit,
            priority,
            ignoreCancelled
        )
        addEntry(eventClass, token, entry)
        return token
    }

    // ---------------------------------------------------------------------------
    // Internal helpers
    // ---------------------------------------------------------------------------

    private fun addEntry(
        eventClass: Class<out SurfEvent>,
        token: Any,
        entry: HandlerEntry
    ) {
        handlers
            .getOrPut(eventClass) { ConcurrentHashMap() }
            .getOrPut(entry.priority) { CopyOnWriteArrayList() }
            .add(entry)

        tokenMap
            .getOrPut(token) { CopyOnWriteArrayList() }
            .add(Pair(eventClass, entry))
    }

    override fun unregisterListeners(listener: Any) {
        val registered = tokenMap.remove(listener) ?: return
        for ((eventClass, entry) in registered) {
            handlers[eventClass]?.get(entry.priority)?.remove(entry)
        }
    }

    // ---------------------------------------------------------------------------
    // Dispatch
    // ---------------------------------------------------------------------------

    @OptIn(InternalInvokerApi::class)
    override suspend fun callAsync(event: SurfAsyncEvent): SurfAsyncEvent {
        val eventHandlers = handlers[event.javaClass] ?: return event

        for (priority in SurfEventPriority.entries) {
            val handlersForPriority = eventHandlers[priority] ?: continue
            for (entry in handlersForPriority) {
                if (entry.ignoreCancelled && event is SurfCancellableEvent && event.isCancelled) continue
                when (entry) {
                    is HandlerEntry.AsyncInvoker -> dispatchAsyncInvoker(entry.invoker, event)
                    is HandlerEntry.AsyncLambda -> entry.handler(event)
                    else -> {} // sync handlers not dispatched here
                }
            }
        }

        return event
    }

    @OptIn(InternalInvokerApi::class)
    override fun callSync(event: SurfSyncEvent): SurfSyncEvent {
        val eventHandlers = handlers[event.javaClass] ?: return event

        for (priority in SurfEventPriority.entries) {
            val handlersForPriority = eventHandlers[priority] ?: continue
            for (entry in handlersForPriority) {
                if (entry.ignoreCancelled && event is SurfCancellableEvent && event.isCancelled) continue
                when (entry) {
                    is HandlerEntry.SyncInvoker -> entry.invoker.invoke(event)
                    is HandlerEntry.SyncLambda -> entry.handler(event)
                    else -> {} // async handlers not dispatched here
                }
            }
        }

        return event
    }

    @OptIn(InternalInvokerApi::class)
    private suspend fun dispatchAsyncInvoker(invoker: SurfAsyncEventInvoker, event: SurfAsyncEvent) {
        suspendCoroutineUninterceptedOrReturn<Any?> { cont ->
            val result = invoker.invoke(event, cont)
            if (result === COROUTINE_SUSPENDED) COROUTINE_SUSPENDED else result
        }
    }
}
