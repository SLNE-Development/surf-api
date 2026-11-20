package dev.slne.surf.api.standalone

import kotlinx.coroutines.runBlocking
import org.springframework.context.SmartLifecycle
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Runs surf-api's hooks in a context that [SurfApiStandaloneBootstrap.adopt] prepared.
 *
 * [start] runs the load and enable hooks, [stop] runs [SurfApiStandaloneBootstrap.shutdown]. A failing start shuts
 * surf-api down before the failure propagates, because the context does not stop a bean whose start failed.
 *
 * Not restartable: [stop] releases surf-api's process-wide state, so a second [start], such as
 * `ApplicationContext.start()` after `stop()`, fails with an [IllegalStateException].
 */
internal class SurfStandaloneLifecycle : SmartLifecycle {

    @Volatile
    private var running = false

    private val startRequested = AtomicBoolean(false)

    override fun start() = runBlocking {
        check(!startRequested.getAndSet(true)) {
            "surf-api cannot be restarted in the same JVM after it stopped or failed to start"
        }

        try {
            SurfApiStandaloneBootstrap.loadAdopted()
            SurfApiStandaloneBootstrap.enable(registerShutdownHook = false)
        } catch (exception: Throwable) {
            runCatching { SurfApiStandaloneBootstrap.shutdown() }
                .exceptionOrNull()
                ?.let(exception::addSuppressed)
            throw exception
        }

        running = true
    }

    override fun stop() = runBlocking {
        running = false
        SurfApiStandaloneBootstrap.shutdown()
    }

    override fun isRunning(): Boolean = running

    override fun getPhase(): Int = SurfApiStandaloneBootstrap.LIFECYCLE_PHASE
}
