package dev.slne.surf.microservice.runtime.lifecycle

import kotlinx.coroutines.runBlocking
import org.springframework.context.SmartLifecycle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Starts and stops a [RuntimeComponent] with the context, in [phase].
 *
 * [stop] runs only after [start] returned normally. A component starts at most once: its [stop]
 * releases what it started for good, so a second [start], such as `ApplicationContext.start()`
 * after `stop()`, fails with an [IllegalStateException].
 */
internal class RuntimeComponentLifecycle(
    private val component: RuntimeComponent,
    private val phase: Int,
) : SmartLifecycle {

    @Volatile
    private var running = false

    private val startRequested = AtomicBoolean(false)

    override fun start() {
        runBlocking {
            check(!startRequested.getAndSet(true)) {
                "The ${component.name} starts at most once and cannot be restarted"
            }
            component.start()
            running = true
        }
    }

    override fun stop() {
        runBlocking {
            if (!running) return@runBlocking

            running = false
            component.stop()
        }
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = phase

    override fun toString(): String = "RuntimeComponentLifecycle(${component.name})"
}
