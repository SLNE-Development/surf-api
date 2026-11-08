package dev.slne.surf.api.core.event

/**
 * Mixin interface for events that can be canceled by a listener.
 */
interface SurfCancellableEvent {
    /**
     * Indicates whether the event is canceled.
     */
    var isCancelled: Boolean

    /**
     *  Cancels the event.
     */
    fun cancel() {
        isCancelled = true
    }
}