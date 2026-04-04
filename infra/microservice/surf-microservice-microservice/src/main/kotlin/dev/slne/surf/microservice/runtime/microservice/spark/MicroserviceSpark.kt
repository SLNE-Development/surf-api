package dev.slne.surf.microservice.runtime.microservice.spark

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.mutableObjectSetOf
import dev.slne.surf.microservice.api.microservice.Microservice
import kotlinx.coroutines.*
import me.lucko.spark.common.SparkPlatform
import me.lucko.spark.common.SparkPlugin
import me.lucko.spark.common.command.sender.CommandSender
import me.lucko.spark.common.platform.PlatformInfo
import me.lucko.spark.standalone.StandaloneCommandSender
import java.lang.Runnable
import java.nio.file.Path
import java.util.logging.Level
import java.util.stream.Stream

object MicroserviceSpark : SparkPlugin {
    override fun getVersion(): String = "1.0.0"
    override fun getPluginDirectory(): Path = Microservice.INSTANCE.dataPath.resolve("spark")

    val platform = SparkPlatform(this)
    private val senders = mutableObjectSetOf<CommandSender>()

    fun onLoad() {
        platform.executeCommand(StandaloneCommandSender.SYSTEM_OUT, arrayOf("profiler", "start"))
    }

    fun onDisable() {
        platform.disable()
    }

    override fun getCommandName(): String? {
        return "spark"
    }

    override fun getCommandSenders(): Stream<out CommandSender> {
        return senders.stream()
    }

    override fun executeAsync(runnable: Runnable?) {
        sparkScope.launch {
            runnable?.run()
        }
    }

    override fun getPlatformInfo(): PlatformInfo {
        return MicroserviceSparkPlatform
    }

    override fun log(level: Level, message: String) {
        log.at(level).log(message)
    }

    override fun log(level: Level, message: String, throwable: Throwable?) {
        log.at(level).withCause(throwable).log(message)
    }

    private val log = logger()
    private val sparkScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineName("spark") + CoroutineExceptionHandler { context, throwable ->
            log.atSevere()
                .withCause(throwable)
                .log("Unhandled exception in Spark plugin coroutine: ${context[CoroutineName.Key]?.name}")
        })
}