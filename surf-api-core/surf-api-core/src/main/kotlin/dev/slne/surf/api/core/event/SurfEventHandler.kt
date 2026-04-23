package dev.slne.surf.api.core.event

/**
 * Marks a method as an event handler that will be invoked by [SurfEventBus]
 * when an event of the matching type is fired.
 *
 * The annotated method must:
 *  - take exactly one parameter, whose type is a [SurfEvent] subclass
 *  - belong to a class instance registered via [SurfEventBus.registerListeners]
 *
 * Additional rules:
 *  - For a [SurfSyncEvent] handler, the method must **not** be a `suspend`
 *    function. This is validated at registration time.
 *  - For a [SurfAsyncEvent] handler, the method **may** be a `suspend`
 *    function, in which case it participates in structured concurrency on the
 *    coroutine that drives [SurfEventBus.callAsync].
 *
 * @property priority dispatch order, from [SurfEventPriority.LOWEST] (first) to
 *   [SurfEventPriority.MONITOR] (last). Defaults to [SurfEventPriority.NORMAL].
 * @property ignoreCancelled when `true`, the handler is skipped if a previous
 *   handler has already marked a [SurfCancellableEvent] as cancelled. Has no
 *   effect for events that do not implement [SurfCancellableEvent].
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class SurfEventHandler(
    val priority: SurfEventPriority = SurfEventPriority.NORMAL,
    val ignoreCancelled: Boolean = false,
)
