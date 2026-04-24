package dev.slne.surf.api.core.event

import dev.slne.surf.api.core.util.requiredService
import org.jetbrains.annotations.ApiStatus

/**
 * Central event bus for cross-plugin event dispatching.
 *
 * Listeners register plain objects whose methods are annotated with [SurfEventHandler].
 * Events must extend either [SurfSyncEvent] (called on the calling thread) or
 * [SurfAsyncEvent] (called inside a coroutine scope).
 */
@ApiStatus.NonExtendable
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

    companion object : SurfEventBus by bus {
        /** The singleton [SurfEventBus] instance. */
        val INSTANCE get() = bus
    }
}

private val bus = requiredService<SurfEventBus>()