package dev.slne.surf.api.core.server.impl.event

import com.google.auto.service.AutoService
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
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentSkipListMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.coroutines.Continuation
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * Default [SurfEventBus] implementation.
 *
 * Handlers are stored in a `Class -> Priority -> List<RegisteredHandler>` map
 * so dispatch can iterate them in priority order without re-sorting on every
 * call. The lookup is performed against every key whose event class is
 * assignable from the concrete event type, which lets handlers registered for
 * a parent event class also receive events of subclasses.
 */
@OptIn(InternalSurfApi::class, InternalInvokerApi::class)
@AutoService(SurfEventBus::class)
class SurfEventBusImpl : SurfEventBus {

    init {
        checkInstantiationByServiceLoader()
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
        ConcurrentHashMap<Class<out SurfEvent>, ConcurrentSkipListMap<SurfEventPriority, MutableList<RegisteredHandler>>>()

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
                list.removeAll { it.listener === listener }
            }
        }
    }

    override fun <T : SurfSyncEvent> registerSyncHandler(
        eventType: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit,
    ): Any {
        @Suppress("UNCHECKED_CAST")
        val typedHandler = handler as (SurfSyncEvent) -> Unit
        val registered = RegisteredHandler(
            listener = typedHandler,
            eventType = eventType,
            priority = priority,
            ignoreCancelled = ignoreCancelled,
            isSuspend = false,
            syncInvoker = SurfSyncEventInvoker { event -> typedHandler(event) },
            asyncInvoker = null,
        )
        addHandler(registered)
        return typedHandler
    }

    override fun <T : SurfAsyncEvent> registerAsyncHandler(
        eventType: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: suspend (T) -> Unit,
    ): Any {
        @Suppress("UNCHECKED_CAST")
        val typedHandler = handler as suspend (SurfAsyncEvent) -> Unit
        // Kotlin compiles `suspend (SurfAsyncEvent) -> Unit` into a
        // `Function2<SurfAsyncEvent, Continuation<Unit>, Any?>` whose `invoke`
        // returns `Unit` for synchronous completion or `COROUTINE_SUSPENDED`
        // when the body actually suspends – which exactly matches the
        // SurfAsyncEventInvoker contract.
        @Suppress("UNCHECKED_CAST")
        val function2 = typedHandler as Function2<SurfAsyncEvent, Continuation<Unit>, Any?>
        val invoker = SurfAsyncEventInvoker { event, continuation ->
            @Suppress("UNCHECKED_CAST")
            function2.invoke(event, continuation as Continuation<Unit>) ?: Unit
        }
        val registered = RegisteredHandler(
            listener = typedHandler,
            eventType = eventType,
            priority = priority,
            ignoreCancelled = ignoreCancelled,
            isSuspend = true,
            syncInvoker = null,
            asyncInvoker = invoker,
        )
        addHandler(registered)
        return typedHandler
    }

    override fun callSync(event: SurfSyncEvent): SurfSyncEvent {
        val matching = collectMatching(event.javaClass) ?: return event
        for (handler in matching) {
            if (shouldSkipForCancellation(event, handler)) continue
            try {
                requireNotNull(handler.syncInvoker) {
                    "Sync invoker missing for ${handler.eventType.name}"
                }.invoke(event)
            } catch (t: Throwable) {
                log.atSevere().withCause(t).log(
                    "Error dispatching sync event %s to handler for %s",
                    event.javaClass.name, handler.eventType.name
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
                val invoker = requireNotNull(handler.asyncInvoker) {
                    "Async invoker missing for ${handler.eventType.name}"
                }
                invokeAsync(invoker, event)
            } catch (t: Throwable) {
                log.atSevere().withCause(t).log(
                    "Error dispatching async event %s to handler for %s",
                    event.javaClass.name, handler.eventType.name
                )
            }
        }
        return event
    }

    private suspend fun invokeAsync(invoker: SurfAsyncEventInvoker, event: SurfAsyncEvent) {
        suspendCoroutineUninterceptedOrReturn<Unit> { cont ->
            val result = invoker.invoke(event, cont)
            if (result === COROUTINE_SUSPENDED) {
                COROUTINE_SUSPENDED
            } else {
                Unit
            }
        }
    }

    private fun shouldSkipForCancellation(event: SurfEvent, handler: RegisteredHandler): Boolean {
        if (!handler.ignoreCancelled) return false
        val cancellable = event as? SurfCancellableEvent ?: return false
        return cancellable.isCancelled
    }

    private fun addHandler(handler: RegisteredHandler) {
        handlers
            .computeIfAbsent(handler.eventType) { ConcurrentSkipListMap() }
            .computeIfAbsent(handler.priority) { CopyOnWriteArrayList() }
            .add(handler)
    }

    /**
     * Returns the flattened list of handlers whose registered event type is
     * assignable from [concreteType], in dispatch order (priority asc, and
     * within a priority the registration order). Returns `null` when there
     * are no handlers, to avoid allocating an empty list on the hot path.
     */
    private fun collectMatching(concreteType: Class<out SurfEvent>): List<RegisteredHandler>? {
        var result: MutableList<RegisteredHandler>? = null
        for ((registeredType, byPriority) in handlers) {
            if (!registeredType.isAssignableFrom(concreteType)) continue
            for ((_, list) in byPriority) {
                if (list.isEmpty()) continue
                val target = result ?: ArrayList<RegisteredHandler>().also { result = it }
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
                    RegisteredHandler(
                        listener = listener,
                        eventType = eventType,
                        priority = annotation.priority,
                        ignoreCancelled = annotation.ignoreCancelled,
                        isSuspend = false,
                        syncInvoker = invoker,
                        asyncInvoker = null,
                    )
                )
            }

            SurfAsyncEvent::class.java.isAssignableFrom(eventType) -> {
                @Suppress("UNCHECKED_CAST")
                val invoker = SurfEventInvokerFactory.asyncFactory.create(
                    listener, method, eventType as Class<out SurfAsyncEvent>
                )
                addHandler(
                    RegisteredHandler(
                        listener = listener,
                        eventType = eventType,
                        priority = annotation.priority,
                        ignoreCancelled = annotation.ignoreCancelled,
                        isSuspend = isSuspend,
                        syncInvoker = null,
                        asyncInvoker = invoker,
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
        val out = ArrayList<HandlerMethod>()
        var current: Class<*>? = type
        while (current != null && current != Any::class.java) {
            for (method in current.declaredMethods) {
                if (method.isSynthetic || method.isBridge) continue
                val annotation = method.getAnnotation(SurfEventHandler::class.java) ?: continue
                val eventType = validateAndExtractEventType(method)
                method.isAccessible = true
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

    private class RegisteredHandler(
        val listener: Any,
        val eventType: Class<out SurfEvent>,
        val priority: SurfEventPriority,
        val ignoreCancelled: Boolean,
        val isSuspend: Boolean,
        val syncInvoker: SurfSyncEventInvoker?,
        val asyncInvoker: SurfAsyncEventInvoker?,
    )

    companion object {
        private val log = logger()
    }
}
