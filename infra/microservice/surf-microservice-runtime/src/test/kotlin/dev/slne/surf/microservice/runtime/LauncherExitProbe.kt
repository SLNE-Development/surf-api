package dev.slne.surf.microservice.runtime

import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceLifecycle
import dev.slne.surf.microservice.runtime.lifecycle.RuntimeComponent
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread
import kotlin.system.exitProcess

/**
 * Runs [runAndExit] with a no-op lifecycle and, once it started, exits the JVM with [SIGNAL_EXIT_CODE] from another
 * thread, as the JVM does on SIGTERM. Prints [STOPPED] when the lifecycle stopped.
 */
object LauncherExitProbe {
    const val SIGNAL_EXIT_CODE = 143
    const val STOPPED = "lifecycle stopped"
    private const val SIGNAL_DELAY_MILLIS = 200L

    @JvmStatic
    fun main(args: Array<String>) {
        val started = CountDownLatch(1)
        val lifecycle = MicroserviceLifecycle(
            registerBuiltinCommands = {},
            createMicroservice = { ProbeMicroservice() },
            surfApi = ProbeComponent("surf-api"),
            spark = ProbeComponent("spark"),
            loadIntegrations = { emptyList() },
            commandEndpoints = ProbeComponent("command endpoints", onStart = started::countDown),
            shutdownLogging = { println(STOPPED) },
        )

        thread(name = "signal") {
            started.await()
            // Lets the launcher suspend in awaitShutdownRequest first.
            Thread.sleep(SIGNAL_DELAY_MILLIS)
            exitProcess(SIGNAL_EXIT_CODE)
        }
        runAndExit(lifecycle, emptyList())
    }

    private class ProbeComponent(override val name: String, private val onStart: () -> Unit = {}) : RuntimeComponent {
        override suspend fun start() = onStart()

        override suspend fun stop() {}
    }

    private class ProbeMicroservice : Microservice() {
        override val name = "probe-service"
    }
}
