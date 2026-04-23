package dev.slne.surf.api.core.event

import dev.slne.surf.api.core.util.requiredService

/**
 * Central event bus for cross-plugin event dispatching.
 *
 * Listeners register plain objects whose methods are annotated with [SurfEventHandler].
 * Events must extend either [SurfSyncEvent] (called on the calling thread) or
 * [SurfAsyncEvent] (called inside a coroutine scope).
 */
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
     * Dispatches [event] to all registered async handlers in priority order and returns it.
     */
    suspend fun callAsync(event: SurfAsyncEvent): SurfAsyncEvent

    /**
     * Dispatches [event] to all registered sync handlers in priority order and returns it.
     */
    fun callSync(event: SurfSyncEvent): SurfSyncEvent

    /**
     * Registers a typed sync [handler] lambda for events of [eventClass].
     *
     * Returns a token that can be passed to [unregisterListeners] to remove the handler.
     */
    fun <T : SurfSyncEvent> registerHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority = SurfEventPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: (T) -> Unit
    ): Any

    /**
     * Registers a typed suspend [handler] lambda for events of [eventClass].
     *
     * Returns a token that can be passed to [unregisterListeners] to remove the handler.
     */
    fun <T : SurfAsyncEvent> registerAsyncHandler(
        eventClass: Class<T>,
        priority: SurfEventPriority = SurfEventPriority.NORMAL,
        ignoreCancelled: Boolean = false,
        handler: suspend (T) -> Unit
    ): Any

    companion object : SurfEventBus by bus {
        /** The singleton [SurfEventBus] instance. */
        val INSTANCE get() = bus
    }
}

private val bus = requiredService<SurfEventBus>()
