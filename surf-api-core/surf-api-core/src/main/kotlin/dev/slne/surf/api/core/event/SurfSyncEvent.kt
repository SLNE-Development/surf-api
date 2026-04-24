package dev.slne.surf.api.core.event


/**
 *  Base class for synchronous surf events dispatched on the calling thread.
 */
abstract class SurfSyncEvent: SurfEvent {

    /**
     * Dispatches the event for handling and determines its resulting state.
     *
     * If the event implements the SurfCancellableEvent interface, the method will return
     * false if the event has been canceled during handling. Otherwise, it will return true.
     *
     * @return true if the event is successfully handled and not canceled, or if the event
     *         is not cancellable; false if the event is cancellable and has been canceled.
     */
    fun call(): Boolean {
        // TODO: call event

        return if (this is SurfCancellableEvent) {
            !isCancelled
        } else {
            true
        }
    }
}