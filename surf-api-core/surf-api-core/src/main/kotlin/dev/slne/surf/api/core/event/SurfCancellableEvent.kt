package dev.slne.surf.api.core.event

/**
 * Marker interface for events that can be cancelled by handlers.
 *
 * Handlers can short-circuit subsequent processing by calling [cancel].
 * Other handlers are still invoked unless they declared
 * `@SurfEventHandler(ignoreCancelled = true)`, in which case they are skipped
 * once the event has been cancelled.
 *
 * Cancellation only inhibits other registered listeners – it does not roll
 * back side effects already performed by earlier handlers, nor does it stop
 * the firing plugin from continuing to process the event after [SurfEventBus]
 * dispatch has finished. The firing plugin is responsible for inspecting
 * [isCancelled] after dispatch.
 */
interface SurfCancellableEvent {

    /** Whether this event has been cancelled by a handler. */
    var isCancelled: Boolean

    /** Convenience for setting [isCancelled] to `true`. */
    fun cancel() {
        isCancelled = true
    }
}
