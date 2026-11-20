package dev.slne.surf.microservice.runtime.lifecycle

/**
 * Part of the runtime that a [RuntimeComponentLifecycle] starts and stops in its phase.
 *
 * [start] runs at most once and [stop] only after [start] returned normally.
 */
internal interface RuntimeComponent {
    /**
     * Name used in logs.
     */
    val name: String

    suspend fun start()

    suspend fun stop()
}
