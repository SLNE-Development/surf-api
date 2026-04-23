package dev.slne.surf.api.core.event

/**
 * Base class for synchronous events dispatched through [SurfEventBus].
 *
 * Handlers for a [SurfSyncEvent] **must not** be declared as `suspend`
 * functions. This is validated when the listener is registered – attempting
 * to register a `suspend` handler for a sync event throws an
 * [IllegalArgumentException].
 *
 * Sync dispatch happens directly on the calling thread without any coroutine
 * overhead.
 *
 * Implementations may also implement [SurfCancellableEvent] in order to be
 * cancellable.
 */
abstract class SurfSyncEvent : SurfEvent
