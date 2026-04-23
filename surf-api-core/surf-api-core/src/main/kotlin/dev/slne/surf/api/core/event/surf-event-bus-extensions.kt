@file:JvmName("SurfEventBusExtensions")
@file:OptIn(InternalSurfApi::class)

package dev.slne.surf.api.core.event

import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Convenience overload that registers multiple listener objects in one call.
 */
fun SurfEventBus.registerListeners(vararg listeners: Any) {
    for (listener in listeners) {
        registerListeners(listener)
    }
}

/**
 * Convenience overload that unregisters multiple listener objects in one call.
 */
fun SurfEventBus.unregisterListeners(vararg listeners: Any) {
    for (listener in listeners) {
        unregisterListeners(listener)
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
 * val token = bus.on<MySyncEvent> { event -> ... }
 * // later
 * bus.unregisterListeners(token)
 * ```
 */
inline fun <reified T : SurfSyncEvent> SurfEventBus.on(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: (T) -> Unit,
): Any = registerSyncHandler(T::class.java, priority, ignoreCancelled) { event ->
    handler(event)
}

/**
 * Registers an inline `suspend` handler for [SurfAsyncEvent]s of type [T]
 * without requiring a dedicated listener class.
 *
 * The returned object can be passed to [SurfEventBus.unregisterListeners] to
 * remove the handler again.
 */
inline fun <reified T : SurfAsyncEvent> SurfEventBus.onAsync(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: suspend (T) -> Unit,
): Any = registerAsyncHandler(T::class.java, priority, ignoreCancelled) { event ->
    handler(event)
}
