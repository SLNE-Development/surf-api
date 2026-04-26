package dev.slne.surf.api.core.server.impl.event

import com.google.auto.service.AutoService
import dev.slne.surf.api.core.event.*
import dev.slne.surf.api.core.invoker.HiddenInvokerUtil
import dev.slne.surf.api.core.server.impl.event.invoker.SurfEventInvokerFactory
import dev.slne.surf.api.core.util.checkInstantiationByServiceLoader
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.shared.api.util.InternalInvokerApi
import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.cancellation.CancellationException

@OptIn(InternalInvokerApi::class)
@AutoService(SurfEventBus::class)
class SurfEventBusImpl : SurfEventBus {
    init {
        checkInstantiationByServiceLoader()
    }

    companion object {
        private val log = logger()
    }

    private val handlers =
        ConcurrentHashMap<Class<out SurfEvent>, ConcurrentHashMap<SurfEventPriority, CopyOnWriteArrayList<SurfEventHandlerEntry>>>()

    private val dispatchCache = ConcurrentHashMap<Class<out SurfEvent>, Array<SurfEventHandlerEntry>>()
    private val emptyHandlers = emptyArray<SurfEventHandlerEntry>()

    private val nextOrder = AtomicLong()

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

        dispatchCache.clear()
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
            ignoreCancelled,
            nextOrder.getAndIncrement()
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
            ignoreCancelled,
            nextOrder.getAndIncrement()
        )
        addHandler(eventClass, entry)

        return token
    }

    override fun callSync(event: SurfSyncEvent): SurfSyncEvent {
        val matching = collectMatching(event.javaClass) ?: return event
        val cancellable = event as? SurfCancellableEvent

        for (handler in matching) {
            if (handler.skipWhenCancelled && cancellable?.isCancelled == true) continue
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
        val cancellable = event as? SurfCancellableEvent

        for (handler in matching) {
            currentCoroutineContext().ensureActive()

            if (handler.skipWhenCancelled && cancellable?.isCancelled == true) continue

            try {
                handler as SurfEventHandlerEntry.AsyncInvoker
                handler.invoker.invoke(event)
            } catch (t: Throwable) {
                // A listener may throw CancellationException on its own. In that case we do not
                // abort event dispatch for later listeners. However, if the calling coroutine
                // context is actually cancelled, ensureActive() rethrows and we stop dispatching.
                if (t is CancellationException) {
                    currentCoroutineContext().ensureActive()
                    log.atWarning()
                        .withCause(t)
                        .log(
                            "Async event handler for %s cancelled itself: %s",
                            event.javaClass.name,
                            handler.token
                        )

                    continue
                }

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

    private fun addHandler(eventType: Class<out SurfEvent>, handler: SurfEventHandlerEntry) {
        handlers
            .computeIfAbsent(eventType) { ConcurrentHashMap() }
            .computeIfAbsent(handler.priority) { CopyOnWriteArrayList() }
            .add(handler)

        dispatchCache.clear()
    }

    private fun collectMatching(concreteType: Class<out SurfEvent>): Array<SurfEventHandlerEntry>? {
        val handlers = dispatchCache.computeIfAbsent(concreteType, ::buildMatchingHandlers)
        return handlers.takeIf { it.isNotEmpty() }
    }

    private fun buildMatchingHandlers(
        concreteType: Class<out SurfEvent>
    ): Array<SurfEventHandlerEntry> {
        val result = ObjectArrayList<SurfEventHandlerEntry>()

        for ((registeredType, byPriority) in handlers) {
            if (!registeredType.isAssignableFrom(concreteType)) continue

            for ((_, list) in byPriority) {
                if (list.isNotEmpty()) {
                    result.addAll(list)
                }
            }
        }

        if (result.isEmpty) return emptyHandlers

        result.sortWith(
            compareBy<SurfEventHandlerEntry> { it.priority.ordinal }
                .thenBy { it.order }
        )

        return result.toTypedArray()
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
                        annotation.ignoreCancelled,
                        nextOrder.getAndIncrement()
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
                        annotation.ignoreCancelled,
                        nextOrder.getAndIncrement()
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