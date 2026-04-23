package dev.slne.surf.api.core.event

/**
 * Base class for asynchronous events dispatched through [SurfEventBus].
 *
 * Handlers for an [SurfAsyncEvent] **may** be declared as `suspend` functions.
 * The bus dispatches all matching handlers sequentially inside a coroutine and
 * awaits their completion before [SurfEventBus.callAsync] returns.
 *
 * Implementations may also implement [SurfCancellableEvent] in order to be
 * cancellable.
 */
abstract class SurfAsyncEvent : SurfEvent
