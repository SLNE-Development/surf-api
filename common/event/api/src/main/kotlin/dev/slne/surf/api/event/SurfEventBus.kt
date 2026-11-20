package dev.slne.surf.api.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import dev.slne.surf.api.spring.SurfContexts
import org.jetbrains.annotations.ApiStatus
import org.springframework.beans.factory.getBean
import org.springframework.context.ApplicationContext
import dev.slne.surf.api.spring.SurfApiBean

/**
 * Central event bus for cross-plugin event dispatching.
 *
 * Listeners register plain objects whose methods are annotated with [SurfEventHandler].
 * Events must extend either [SurfSyncEvent] (called on the calling thread) or
 * [SurfAsyncEvent] (called inside a coroutine scope).
 *
 * ## Polymorphic dispatch
 *
 * Handler matching is type-hierarchy-aware: a handler declared for event type `A` will
 * also be invoked when a subtype `B : A` is dispatched. This mirrors standard
 * `instanceof`/`isAssignableFrom` semantics. For example:
 *
 * ```kotlin
 * // Given:
 * open class BaseEvent : SurfAsyncEvent()
 * class SpecificEvent : BaseEvent()
 *
 * // A handler for BaseEvent is triggered by both BaseEvent and SpecificEvent:
 * @SurfEventHandler
 * suspend fun onBase(event: BaseEvent) { /* called for SpecificEvent too */ }
 *
 * @SurfEventHandler
 * suspend fun onSpecific(event: SpecificEvent) { /* only called for SpecificEvent */ }
 * ```
 *
 * When `SpecificEvent` is dispatched both handlers are invoked (in priority order).
 * When `BaseEvent` is dispatched only `onBase` is invoked.
 */
@ApiStatus.NonExtendable
@SurfApiBean
interface SurfEventBus {

    /**
     * Registers all [SurfEventHandler]-annotated methods on [listener] with this bus.
     */
    fun registerListeners(listener: Any)

    /**
     * Removes all handlers previously registered for [listener].
     */
    fun unregisterListeners(listener: Any)

    /**
     * Dispatches [event] to every registered handler in priority order and
     * suspends until all of them have completed.
     *
     * Handlers are invoked sequentially. A handler that throws does not stop
     * other handlers from running – the throwable is logged and dispatching
     * continues.
     *
     * For [SurfCancellableEvent]s, handlers marked with
     * [SurfEventHandler.ignoreCancelled] may be skipped after cancellation,
     * except handlers with MONITOR priority, which are
     * always invoked.
     *
     * @return the same [event] instance, for fluent chaining.
     */
    suspend fun callAsync(event: SurfAsyncEvent): SurfAsyncEvent

    /**
     * Dispatches [event] to every registered handler in priority order on the
     * calling thread.
     *
     * Handlers are invoked sequentially. A handler that throws does not stop
     * other handlers from running – the throwable is logged and dispatching
     * continues.
     *
     * For [SurfCancellableEvent]s, handlers marked with
     * [SurfEventHandler.ignoreCancelled] may be skipped after cancellation,
     * except handlers with MONITOR priority, which are
     * always invoked.
     *
     * @return the same [event] instance, for fluent chaining.
     */
    fun callSync(event: SurfSyncEvent): SurfSyncEvent

    /**
     * Registers a typed sync [handler] lambda for events of [eventClass].
     *
     * The handler is invoked for every dispatched event whose runtime type is
     * [eventClass] **or any subclass thereof** (polymorphic dispatch).
     *
     * If [ignoreCancelled] is `true`, the handler is skipped once a
     * [SurfCancellableEvent] has been cancelled. Handlers with
     * MONITOR priority are always called.
     *
     * Returns a token that can be passed to [unregisterListeners] to remove the handler.
     */
    fun <T : SurfSyncEvent> registerHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority = SurfEventPriority.NORMAL,
        ignoreCancelled: Boolean = true,
        handler: (T) -> Unit
    ): Any

    /**
     * Registers a typed suspend [handler] lambda for events of [eventClass].
     *
     * The handler is invoked for every dispatched event whose runtime type is
     * [eventClass] **or any subclass thereof** (polymorphic dispatch).
     *
     * If [ignoreCancelled] is `true`, the handler is skipped once a
     * [SurfCancellableEvent] has been cancelled. Handlers with
     * MONITOR priority are always called.
     *
     * Returns a token that can be passed to [unregisterListeners] to remove the handler.
     */
    fun <T : SurfAsyncEvent> registerAsyncHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority = SurfEventPriority.NORMAL,
        ignoreCancelled: Boolean = true,
        handler: suspend (T) -> Unit
    ): Any

    companion object {
        /**
         * The [SurfEventBus] bean of surf-api's installed root context.
         *
         * Resolved again after the platform installs a new root context, so that it is the bus the
         * root's beans inject. While no root context is installed, the last resolved bus stays in
         * use.
         *
         * @throws IllegalStateException if no root context is installed and no bus was resolved
         *   before
         */
        @InternalSurfApi
        val INSTANCE: SurfEventBus get() = RootEventBus.current()
    }
}

/**
 * Caches the [SurfEventBus] bean per installed root context.
 */
private object RootEventBus {

    private class Resolved(val root: ApplicationContext, val bus: SurfEventBus)

    @Volatile
    private var resolved: Resolved? = null

    fun current(): SurfEventBus {
        val cached = resolved
        val installed = SurfContexts.rootOrNull
        if (cached != null && (installed == null || installed === cached.root)) {
            return cached.bus
        }

        val root = SurfContexts.root
        return Resolved(root, root.getBean<SurfEventBus>()).also { resolved = it }.bus
    }
}

/**
 * Registers an inline handler for [SurfSyncEvent]s of type [T] without
 * requiring a dedicated listener class.
 *
 * The returned object can be passed to [SurfEventBus.unregisterListeners] to
 * remove the handler again.
 *
 * Example:
 * ```
 * // eventBus is an injected SurfEventBus
 * val token = eventBus.on<MySyncEvent> { event -> ... }
 * // later
 * eventBus.unregisterListeners(token)
 * ```
 * If [ignoreCancelled] is `true`, the handler is skipped once a
 * [SurfCancellableEvent] has been cancelled. Handlers with
 * MONITOR priority are always called.
 */
inline fun <reified T : SurfSyncEvent> SurfEventBus.on(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = true,
    crossinline handler: (T) -> Unit,
): Any = registerHandler(T::class.java, priority, ignoreCancelled) { event ->
    handler(event)
}

/**
 * Registers an inline `suspend` handler for [SurfAsyncEvent]s of type [T]
 * without requiring a dedicated listener class.
 *
 * The returned object can be passed to [SurfEventBus.unregisterListeners] to
 * remove the handler again.
 *
 * If [ignoreCancelled] is `true`, the handler is skipped once a
 * [SurfCancellableEvent] has been cancelled. Handlers with
 * MONITOR priority are always called.
 */
inline fun <reified T : SurfAsyncEvent> SurfEventBus.onAsync(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = true,
    crossinline handler: suspend (T) -> Unit,
): Any = registerAsyncHandler(T::class.java, priority, ignoreCancelled) { event ->
    handler(event)
}
