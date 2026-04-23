package dev.slne.surf.api.core.event

/** Mixin interface for events that can be cancelled by a listener. */
interface SurfCancellableEvent {
    var isCancelled: Boolean

    /** Cancels the event. */
    fun cancel() {
        isCancelled = true
    }
}
