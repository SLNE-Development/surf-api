package dev.slne.surf.api.core.event

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Central, platform-independent event bus used by every surf-* plugin.
 *
 * Plugins define their own [SurfEvent] subclasses (either [SurfSyncEvent] or
 * [SurfAsyncEvent]) and fire them through [callSync] / [callAsync]. Other
 * plugins can listen for these events without having a direct dependency on
 * the firing plugin.
 *
 * A "listener" is any object with one or more methods annotated with
 * [SurfEventHandler]. Each handler method takes exactly one parameter, the
 * event type. Sync-event handlers must not be `suspend` functions; async-event
 * handlers may be.
 *
 * The bus deliberately **does not** integrate with Bukkit's `EventBus` or
 * Velocity's `EventManager` – the existing platform event infrastructure stays
 * in place; this bus coexists with it.
 */
interface SurfEventBus {

    /**
     * Registers all [SurfEventHandler]-annotated methods on [listener] with
     * this bus.
     *
     * Validation performed at registration:
     *  - every handler method must take exactly one parameter
     *  - that parameter type must be a subclass of [SurfEvent]
     *  - if the parameter type is a [SurfSyncEvent], the method must not be a
     *    `suspend` function
     *
     * Throws [IllegalArgumentException] if validation fails.
     *
     * Calling this method multiple times for the same listener instance
     * registers the handlers multiple times.
     */
    fun registerListeners(listener: Any)

    /**
     * Removes every handler that was registered for [listener] from this bus.
     *
     * If [listener] was not previously registered, this is a no-op.
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
     * @return the same [event] instance, for fluent chaining.
     */
    fun callSync(event: SurfSyncEvent): SurfSyncEvent

    /**
     * Internal entry point used by the inline [on] DSL to register a
     * lambda-based [SurfSyncEvent] handler with full control over event type,
     * priority and `ignoreCancelled`.
     *
     * Returns an opaque token that can be passed to [unregisterListeners] to
     * remove the handler again. Consumers should not depend on the concrete
     * runtime type of the returned token.
     */
    @InternalSurfApi
    fun <T : SurfSyncEvent> registerSyncHandler(
        eventType: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: (T) -> Unit,
    ): Any

    /**
     * Internal entry point used by the inline [onAsync] DSL to register a
     * lambda-based [SurfAsyncEvent] handler with full control over event
     * type, priority and `ignoreCancelled`.
     *
     * Returns an opaque token that can be passed to [unregisterListeners] to
     * remove the handler again. Consumers should not depend on the concrete
     * runtime type of the returned token.
     */
    @InternalSurfApi
    fun <T : SurfAsyncEvent> registerAsyncHandler(
        eventType: Class<T>,
        priority: SurfEventPriority,
        ignoreCancelled: Boolean,
        handler: suspend (T) -> Unit,
    ): Any

    companion object : SurfEventBus by api {

        /** Direct access to the singleton instance. */
        @JvmStatic
        val instance: SurfEventBus get() = api
    }
}

private val api = requiredService<SurfEventBus>()
