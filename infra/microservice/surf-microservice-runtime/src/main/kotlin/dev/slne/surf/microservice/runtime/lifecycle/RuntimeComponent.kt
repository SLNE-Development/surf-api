package dev.slne.surf.microservice.runtime.lifecycle

/**
 * Part of the runtime that [MicroserviceLifecycle] starts and stops at a fixed point of its order.
 *
 * The lifecycle calls [start] at most once and [stop] only after [start] returned normally.
 */
internal interface RuntimeComponent {
    /**
     * Name used in logs.
     */
    val name: String

    suspend fun start()

    suspend fun stop()
}
