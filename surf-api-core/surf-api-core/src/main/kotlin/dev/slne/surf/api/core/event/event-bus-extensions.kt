package dev.slne.surf.api.core.event

/**
 * Registers multiple [listeners] at once.
 */
fun SurfEventBus.registerListeners(vararg listeners: Any) {
    listeners.forEach { registerListeners(it) }
}

/**
 * Registers an inline sync handler for events of type [T] and returns the listener token.
 *
 * The returned token can be passed to [SurfEventBus.unregisterListeners] to remove the handler.
 */
inline fun <reified T : SurfSyncEvent> SurfEventBus.on(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: (T) -> Unit
): Any = registerHandler(T::class.java, priority, ignoreCancelled) { handler(it) }

/**
 * Registers an inline suspend handler for events of type [T] and returns the listener token.
 *
 * The returned token can be passed to [SurfEventBus.unregisterListeners] to remove the handler.
 */
inline fun <reified T : SurfAsyncEvent> SurfEventBus.onAsync(
    priority: SurfEventPriority = SurfEventPriority.NORMAL,
    ignoreCancelled: Boolean = false,
    crossinline handler: suspend (T) -> Unit
): Any = registerAsyncHandler(T::class.java, priority, ignoreCancelled) { handler(it) }
