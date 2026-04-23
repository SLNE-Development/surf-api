package dev.slne.surf.api.core.event

/**
 * Marks a method as a [SurfEventBus] event handler.
 *
 * The annotated method must have exactly one parameter whose type extends either
 * [SurfSyncEvent] or [SurfAsyncEvent]. Handlers for [SurfAsyncEvent] may be `suspend` functions.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class SurfEventHandler(
    /** Execution priority relative to other handlers for the same event type. */
    val priority: SurfEventPriority = SurfEventPriority.NORMAL,
    /** When `true`, this handler is skipped if the event has already been cancelled. */
    val ignoreCancelled: Boolean = false
)
