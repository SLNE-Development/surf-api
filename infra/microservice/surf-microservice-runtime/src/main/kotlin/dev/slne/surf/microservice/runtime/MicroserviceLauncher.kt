package dev.slne.surf.microservice.runtime

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import dev.slne.surf.microservice.api.integration.MicroserviceIntegration
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandEndpoints
import dev.slne.surf.microservice.runtime.command.builtin.builtinCommands
import dev.slne.surf.microservice.runtime.command.console.MicroserviceConsoleListener
import dev.slne.surf.microservice.runtime.command.transport.MicroserviceCommandServer
import dev.slne.surf.microservice.runtime.command.transport.MicroserviceCommandSocket
import dev.slne.surf.microservice.runtime.discovery.MicroserviceMainClass
import dev.slne.surf.microservice.runtime.health.MicroserviceHealthManager
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceLifecycle
import dev.slne.surf.microservice.runtime.lifecycle.SurfApiComponent
import dev.slne.surf.microservice.runtime.logging.Slf4jProvider
import dev.slne.surf.microservice.runtime.spark.MicroserviceSpark
import kotlinx.coroutines.runBlocking
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private const val START_FAILURE_EXIT_CODE = 1
private const val NORMAL_EXIT_CODE = 0
private val COMMAND_STOP_TIMEOUT = 5.seconds

/**
 * Runs the microservice named in `META-INF/surf-microservice/main` until a command or a JVM shutdown signal requests
 * a shutdown, see [runAndExit]. The command-line [args] reach [Microservice.onBootstrap].
 *
 * The environment variables that configure a running microservice:
 * - `SURF_MICROSERVICE_NAME`: the service name, see [Microservice.name].
 * - `SURF_MICROSERVICE_CONFIG_DIR`: the data directory, see [Microservice.dataPath].
 * - `SURF_MICROSERVICE_COMMAND_SOCKET`: the path of the command socket, see [MicroserviceCommandSocket.resolvePath].
 * - `SURF_MICROSERVICE_SPARK_PROFILER`: `true` starts the spark profiler together with the microservice.
 *
 * Nothing may log before [Slf4jProvider.selectLog4j] runs, so this file declares no logger property.
 */
fun main(args: Array<String>) {
    Slf4jProvider.selectLog4j()
    runAndExit(createLifecycle(), args.asList())
}

/**
 * Starts [lifecycle], waits for a shutdown request, shuts it down and exits the process.
 *
 * Owns the only JVM shutdown hook of the process. surf-api registers none, and log4j registers none because the
 * bundled `log4j2.xml` sets `shutdownHook="disable"`. A replacement `log4j2.xml` must set the same attribute, because
 * the lifecycle shuts logging down itself as its last step. The hook only runs
 * [MicroserviceLifecycle.shutdown]. The lifecycle runs in [runBlocking] on the calling thread, so coroutines that the
 * hook resumes continue on that thread, and only that thread calls [exitProcess]: `Runtime.exit` from a shutdown
 * hook blocks forever.
 *
 * A failed start exits with code `1` after stopping what already started. After a shutdown requested by a command the
 * process exits with code `0`, even if a library left non-daemon threads behind. After a JVM shutdown signal the
 * calling thread blocks in [exitProcess] until the hook completes and the JVM halts with the signal's exit code.
 */
internal fun runAndExit(lifecycle: MicroserviceLifecycle, args: List<String>): Nothing {
    val shutdownHook = thread(start = false, name = "MicroserviceShutdown") {
        runBlocking {
            lifecycle.shutdown()
        }
    }
    Runtime.getRuntime().addShutdownHook(shutdownHook)

    val exitCode = runBlocking { runUntilShutdown(lifecycle, args) }
    removeShutdownHook(shutdownHook)
    exitProcess(exitCode)
}

private suspend fun runUntilShutdown(lifecycle: MicroserviceLifecycle, args: List<String>): Int {
    try {
        lifecycle.start(args)
    } catch (exception: Throwable) {
        logger().atSevere()
            .withCause(exception)
            .log("The microservice failed to start")
        lifecycle.shutdown()
        return START_FAILURE_EXIT_CODE
    }

    lifecycle.awaitShutdownRequest()
    lifecycle.shutdown()
    return NORMAL_EXIT_CODE
}

private fun createLifecycle(): MicroserviceLifecycle {
    val classLoader = Microservice::class.java.classLoader
    val spark = MicroserviceSpark()
    val healthManager = MicroserviceHealthManager()
    val dispatcher = MicroserviceCommandDispatcher()

    lateinit var lifecycle: MicroserviceLifecycle
    val requestShutdown = { lifecycle.requestShutdown() }
    val commandEndpoints = MicroserviceCommandEndpoints(
        server = MicroserviceCommandServer(
            dispatcher = dispatcher,
            requestShutdown = requestShutdown,
            socketPath = MicroserviceCommandSocket.resolvePath(),
            stopTimeout = COMMAND_STOP_TIMEOUT,
        ),
        console = MicroserviceConsoleListener(dispatcher, requestShutdown, COMMAND_STOP_TIMEOUT),
    )

    lifecycle = MicroserviceLifecycle(
        registerBuiltinCommands = {
            val builtin = builtinCommands(healthManager, spark)
            builtin.forEach(MicroserviceCommands::register)
        },
        createMicroservice = { MicroserviceMainClass.instantiate(classLoader) },
        surfApi = SurfApiComponent,
        spark = spark,
        loadIntegrations = {
            ServiceLoader.load(MicroserviceIntegration::class.java, classLoader).toList()
        },
        commandEndpoints = commandEndpoints,
        shutdownLogging = LogManager::shutdown,
    )

    return lifecycle
}

/**
 * Removes [hook] unless the JVM already runs it, which happens when a shutdown signal arrived meanwhile.
 */
private fun removeShutdownHook(hook: Thread) {
    try {
        Runtime.getRuntime().removeShutdownHook(hook)
    } catch (_: IllegalStateException) {
    }
}
