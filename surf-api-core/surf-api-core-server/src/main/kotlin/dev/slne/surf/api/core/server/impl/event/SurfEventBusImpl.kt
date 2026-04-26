package dev.slne.surf.api.core.server.impl.event

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.event.*
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil
import dev.slne.surf.api.core.server.impl.event.invoker.SurfEventInvokerFactory
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList

@OptIn(InternalInvokerApi::class)
@AutoService(SurfEventBus::class)
class SurfEventBusImpl : SurfEventBus {
    init {
        checkInstantiationByServiceLoader()
    }

    companion object {
        private val log = logger()
    }

    /**
     * Per-event-class map of registered handlers, grouped by priority. Outer
     * map and inner skip-list map are concurrent; the per-priority list uses
     * [CopyOnWriteArrayList] so dispatch can iterate without copying or
     * locking. The skip-list map keeps entries naturally ordered by priority,
     * which we re-sort across multiple matching event types in
     * [collectMatching].
     */
    private val handlers =
        ConcurrentHashMap<Class<out SurfEvent>, ConcurrentSkipListMap<SurfEventPriority, CopyOnWriteArrayList<SurfEventHandlerEntry>>>()

    override fun registerListeners(listener: Any) {
        val handlerMethods = collectHandlerMethods(listener.javaClass)
        require(handlerMethods.isNotEmpty()) {
            "No @SurfEventHandler methods found on ${listener.javaClass.name}"
        }
        for ((method, annotation, eventType) in handlerMethods) {
            registerMethodHandler(listener, method, annotation, eventType)
        }
    }

    override fun unregisterListeners(listener: Any) {
        for ((_, byPriority) in handlers) {
            for ((_, list) in byPriority) {
                list.removeAll { it.token === listener }
            }
        }
    }

    override fun <T : SurfSyncEvent> registerHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit
    ): Any {
        val token = Any()

        @Suppress("UNCHECKED_CAST")
        val entry = SurfEventHandlerEntry.SyncInvoker(
            token,
            handler as (SurfSyncEvent) -> Unit,
            priority,
            ignoreCancelled
        )
        addHandler(eventClass, entry)

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
        val entry = SurfEventHandlerEntry.AsyncInvoker(
            token,
            handler as suspend (SurfAsyncEvent) -> Unit,
            priority,
            ignoreCancelled
        )
        addHandler(eventClass, entry)

        return token
    }

    override fun callSync(event: SurfSyncEvent): SurfSyncEvent {
        val matching = collectMatching(event.javaClass) ?: return event

        for (handler in matching) {
            if (shouldSkipForCancellation(event, handler)) continue
            try {
                handler as SurfEventHandlerEntry.SyncInvoker
                handler.invoker.invoke(event)
            } catch (t: Throwable) {
                log.atSevere()
                    .withCause(t)
                    .log(
                        "Error dispatching sync event %s to handler for %s",
                        event.javaClass.name, handler.token
                    )
            }
        }

        return event
    }

    override suspend fun callAsync(event: SurfAsyncEvent): SurfAsyncEvent {
        val matching = collectMatching(event.javaClass) ?: return event

        for (handler in matching) {
            if (shouldSkipForCancellation(event, handler)) continue
            try {
                handler as SurfEventHandlerEntry.AsyncInvoker
                handler.invoker.invoke(event)
            } catch (t: Throwable) {
                log.atSevere()
                    .withCause(t)
                    .log(
                        "Error dispatching async event %s to handler for %s",
                        event.javaClass.name, handler.token
                    )
            }
        }

        return event
    }

    private fun shouldSkipForCancellation(event: SurfEvent, handler: SurfEventHandlerEntry): Boolean {
        if (!handler.ignoreCancelled || handler.priority == SurfEventPriority.MONITOR) return false
        val cancellable = event as? SurfCancellableEvent ?: return false
        return cancellable.isCancelled
    }

    private fun addHandler(eventType: Class<out SurfEvent>, handler: SurfEventHandlerEntry) {
        handlers
            .computeIfAbsent(eventType) { ConcurrentSkipListMap() }
            .computeIfAbsent(handler.priority) { CopyOnWriteArrayList() }
            .add(handler)
    }

    /**
     * Returns the flattened list of handlers whose registered event type is
     * assignable from [concreteType], in dispatch order (priority asc, and
     * within a priority the registration order). Returns `null` when there
     * are no handlers, to avoid allocating an empty list on the hot path.
     */
    private fun collectMatching(concreteType: Class<out SurfEvent>): List<SurfEventHandlerEntry>? {
        var result: MutableList<SurfEventHandlerEntry>? = null
        for ((registeredType, byPriority) in handlers) {
            if (!registeredType.isAssignableFrom(concreteType)) continue
            for ((_, list) in byPriority) {
                if (list.isEmpty()) continue
                val target = result ?: ObjectArrayList<SurfEventHandlerEntry>().also { result = it }
                target.addAll(list)
            }
        }
        result?.sortBy { it.priority.ordinal }
        return result
    }

    private fun registerMethodHandler(
        listener: Any,
        method: Method,
        annotation: SurfEventHandler,
        eventType: Class<out SurfEvent>,
    ) {
        val isSuspend = HiddenInvokerUtil.isSuspendFunction(method)

        when {
            SurfSyncEvent::class.java.isAssignableFrom(eventType) -> {
                require(!isSuspend) {
                    "@SurfEventHandler ${method.declaringClass.name}.${method.name} " +
                            "is for SurfSyncEvent ${eventType.name} and must not be suspend"
                }

                @Suppress("UNCHECKED_CAST")
                val invoker = SurfEventInvokerFactory.syncFactory.create(
                    listener, method, eventType as Class<out SurfSyncEvent>
                )
                addHandler(
                    eventType,
                    SurfEventHandlerEntry.SyncInvoker(
                        listener,
                        invoker,
                        annotation.priority,
                        annotation.ignoreCancelled
                    )
                )
            }

            SurfAsyncEvent::class.java.isAssignableFrom(eventType) -> {
                @Suppress("UNCHECKED_CAST")
                val invoker = SurfEventInvokerFactory.asyncFactory.create(
                    listener, method, eventType as Class<out SurfAsyncEvent>
                )
                addHandler(
                    eventType,
                    SurfEventHandlerEntry.AsyncInvoker(
                        listener,
                        invoker,
                        annotation.priority,
                        annotation.ignoreCancelled
                    )
                )
            }

            else -> error(
                "@SurfEventHandler ${method.declaringClass.name}.${method.name} parameter " +
                        "type ${eventType.name} is not a SurfSyncEvent or SurfAsyncEvent"
            )
        }
    }

    /**
     * Walks every public/private method declared on [type] (and its
     * superclasses) and returns the ones annotated with [SurfEventHandler],
     * after validating their signature.
     */
    private fun collectHandlerMethods(type: Class<*>): List<HandlerMethod> {
        val out = ObjectArrayList<HandlerMethod>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (method.isSynthetic || method.isBridge) continue
                val annotation = method.getAnnotation(SurfEventHandler::class.java) ?: continue
                val eventType = validateAndExtractEventType(method)
                out += HandlerMethod(method, annotation, eventType)
            }
            current = current.superclass
        }
        return out
    }

    private fun validateAndExtractEventType(method: Method): Class<out SurfEvent> {
        val params = method.parameterTypes
        // Suspend functions have a trailing Continuation parameter, which we
        // accept here – the rest of the signature must still describe exactly
        // one event parameter.
        val effectiveParamCount = if (HiddenInvokerUtil.isSuspendFunction(method)) {
            params.size - 1
        } else {
            params.size
        }
        require(effectiveParamCount == 1) {
            "@SurfEventHandler ${method.declaringClass.name}.${method.name} must take exactly " +
                    "one parameter (the event type), but takes $effectiveParamCount"
        }
        val eventParam = params[0]
        require(SurfEvent::class.java.isAssignableFrom(eventParam)) {
            "@SurfEventHandler ${method.declaringClass.name}.${method.name} parameter type " +
                    "${eventParam.name} must be a subclass of SurfEvent"
        }
        @Suppress("UNCHECKED_CAST")
        return eventParam as Class<out SurfEvent>
    }

    private data class HandlerMethod(
        val method: Method,
        val annotation: SurfEventHandler,
        val eventType: Class<out SurfEvent>,
    )
}