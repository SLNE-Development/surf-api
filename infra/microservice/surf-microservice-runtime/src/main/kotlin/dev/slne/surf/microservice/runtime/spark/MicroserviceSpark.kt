package dev.slne.surf.microservice.runtime.spark

import dev.slne.surf.api.core.environment.env
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.microservice.api.Microservice
import dev.slne.surf.microservice.runtime.MicroserviceRuntimeBuildConfig
import dev.slne.surf.microservice.runtime.command.LoggingCommandOutput
import dev.slne.surf.microservice.runtime.lifecycle.RuntimeComponent
import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import me.lucko.spark.common.SparkPlatform
import me.lucko.spark.common.SparkPlugin
import me.lucko.spark.common.command.sender.CommandSender
import me.lucko.spark.common.platform.PlatformInfo
import java.lang.Runnable
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.logging.Level
import java.util.stream.Stream
import kotlin.coroutines.cancellation.CancellationException

/**
 * Embeds spark for its commands, such as `spark health`, and starts the sampling profiler with the service only if
 * [PROFILER_ENVIRONMENT_VARIABLE] is `true`. `spark profiler start` starts it on demand. A profiler that fails to
 * start is logged and does not fail the start. A value of the variable other than `true` or `false` fails the start
 * before spark is enabled.
 *
 * The profiler is off by default because its call tree stays on the heap and keeps idle services large.
 *
 * Starts after surf-api because spark keeps its files in `<dataPath>/spark` of [Microservice.instance]. Commands
 * are accepted only while spark runs. spark's static log output goes through this plugin's logger. The
 * [CancellationException]s that spark's executors report while [stop] cancels the profiler are logged at debug level.
 */
internal class MicroserviceSpark : SparkPlugin, RuntimeComponent {
    override val name = "spark"

    private val log = logger()
    private val senders = ConcurrentHashMap.newKeySet<CommandSender>()
    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.IO + CoroutineName("spark") +
                CoroutineExceptionHandler { context, exception ->
                    log.atSevere()
                        .withCause(exception)
                        .log("Unhandled exception in spark coroutine %s", context[CoroutineName])
                },
    )

    @Volatile
    private var platform: SparkPlatform? = null

    @Volatile
    private var stopping = false

    val isRunning: Boolean
        get() = platform != null

    override suspend fun start() {
        val profileOnStart = env.boolean(PROFILER_ENVIRONMENT_VARIABLE, default = false)

        if (!SparkStaticLogging.redirectTo(this)) {
            log.atWarning()
                .log("Could not route spark's static log output; it goes to the console unformatted")
        }

        val platform = SparkPlatform(this)
        platform.enable()
        this.platform = platform

        if (profileOnStart) {
            startProfiler()
        }
    }

    private suspend fun startProfiler() {
        try {
            executeCommand(MicroserviceSparkCommandSender(LoggingCommandOutput), PROFILER_START)
        } catch (exception: Exception) {
            if (exception is CancellationException) throw exception
            log.atWarning()
                .withCause(exception)
                .log("Could not start the spark profiler")
        }
    }

    override suspend fun stop() {
        stopping = true
        try {
            platform?.disable()
            platform = null
        } finally {
            scope.cancel("spark disabled")
        }
    }

    /**
     * Runs a `spark` command with [args] for [sender] and suspends until it completes.
     *
     * @throws IllegalStateException if spark is not running
     */
    suspend fun executeCommand(sender: CommandSender, args: List<String>) {
        val platform = checkNotNull(platform) { "spark is not running" }

        senders.add(sender)
        try {
            platform.executeCommand(sender, args.toTypedArray()).await()
        } finally {
            senders.remove(sender)
        }
    }

    override fun getVersion(): String = MicroserviceRuntimeBuildConfig.VERSION

    override fun getPluginDirectory(): Path = Microservice.instance.dataPath.resolve(DIRECTORY_NAME)

    override fun getCommandName(): String = COMMAND_NAME

    override fun getCommandSenders(): Stream<out CommandSender> = senders.stream()

    override fun executeAsync(task: Runnable) {
        scope.launch {
            runInterruptible {
                task.run()
            }
        }
    }

    override fun getPlatformInfo(): PlatformInfo = MicroserviceSparkPlatformInfo

    override fun log(level: Level, message: String) {
        log.at(level).log("%s", message)
    }

    override fun log(level: Level, message: String, throwable: Throwable) {
        val effectiveLevel = if (isShutdownCancellation(throwable)) Level.FINE else level
        log.at(effectiveLevel)
            .withCause(throwable)
            .log("%s", message)
    }

    /**
     * spark's executors report a periodic task that was cancelled while running, which happens when [stop] cancels
     * the profiler, as an uncaught exception.
     */
    private fun isShutdownCancellation(throwable: Throwable): Boolean {
        return stopping && throwable is CancellationException
    }

    private companion object {
        /**
         * Environment variable that starts the profiler with the microservice when set to `true`.
         */
        const val PROFILER_ENVIRONMENT_VARIABLE = "SURF_MICROSERVICE_SPARK_PROFILER"

        const val COMMAND_NAME = "spark"
        const val DIRECTORY_NAME = "spark"
        val PROFILER_START = listOf("profiler", "start")
    }
}