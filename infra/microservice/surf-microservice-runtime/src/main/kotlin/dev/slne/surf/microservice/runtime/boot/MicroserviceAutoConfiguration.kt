package dev.slne.surf.microservice.runtime.boot

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.standalone.SurfApiStandaloneBootstrap
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.api.command.MicroserviceCommand
import dev.slne.surf.microservice.api.command.MicroserviceCommands
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandDispatcher
import dev.slne.surf.microservice.runtime.command.MicroserviceCommandEndpoints
import dev.slne.surf.microservice.runtime.command.builtin.builtinCommands
import dev.slne.surf.microservice.runtime.command.builtin.registerBuiltinCommands
import dev.slne.surf.microservice.runtime.command.console.MicroserviceConsoleListener
import dev.slne.surf.microservice.runtime.command.transport.MicroserviceCommandServer
import dev.slne.surf.microservice.runtime.command.transport.MicroserviceCommandSocket
import dev.slne.surf.microservice.runtime.health.MicroserviceHealth
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceShutdown
import dev.slne.surf.microservice.runtime.lifecycle.RuntimeComponentLifecycle
import dev.slne.surf.microservice.runtime.spark.MicroserviceSpark
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.health.registry.HealthContributorRegistry
import org.springframework.context.annotation.Bean
import kotlin.time.Duration.Companion.seconds

/**
 * Provides the runtime of a microservice that [runMicroservice][dev.slne.surf.microservice.runtime.runMicroservice]
 * started: the command endpoints, the built-in commands, spark and the health checks.
 *
 * The built-in commands, unless a registered command already uses one of their labels, and every `MicroserviceCommand`
 * bean are registered in [MicroserviceCommands] once all singletons exist. spark starts before surf-api's hooks run;
 * the console and the command socket start after them and stop before them, so commands only run while the
 * application is enabled. The health checks run every `HealthIndicator` bean.
 */
@AutoConfiguration
@ConditionalOnBean(MicroserviceInfo::class)
class MicroserviceAutoConfiguration {

    private val log = logger()

    @Bean(HEALTH_BEAN)
    internal fun microserviceHealth(registry: ObjectProvider<HealthContributorRegistry>): MicroserviceHealth {
        return MicroserviceHealth({ registry.ifAvailable })
    }

    @Bean(SPARK_BEAN)
    internal fun microserviceSpark(info: MicroserviceInfo) = MicroserviceSpark(info.dataPath)

    @Bean(SPARK_LIFECYCLE_BEAN)
    internal fun microserviceSparkLifecycle(spark: MicroserviceSpark) = RuntimeComponentLifecycle(spark, SPARK_PHASE)

    @Bean(COMMAND_REGISTRATION_BEAN)
    internal fun microserviceCommandRegistration(
        health: MicroserviceHealth,
        spark: MicroserviceSpark,
        commands: ObjectProvider<MicroserviceCommand>,
    ) = SmartInitializingSingleton {
        registerBuiltinCommands(builtinCommands(health, spark)).forEach { (command, takenLabel) ->
            log.atWarning().log(
                "Built-in command '%s' is not registered: the label '%s' is already taken",
                command.name,
                takenLabel,
            )
        }
        commands.orderedStream().forEach(MicroserviceCommands::register)
    }

    @Bean(COMMAND_ENDPOINTS_BEAN)
    internal fun microserviceCommandEndpoints(shutdown: MicroserviceShutdown): RuntimeComponentLifecycle {
        val dispatcher = MicroserviceCommandDispatcher()
        val endpoints = MicroserviceCommandEndpoints(
            server = MicroserviceCommandServer(
                dispatcher = dispatcher,
                requestShutdown = shutdown::request,
                socketPath = MicroserviceCommandSocket.resolvePath(),
                stopTimeout = COMMAND_STOP_TIMEOUT,
            ),
            console = MicroserviceConsoleListener(dispatcher, shutdown::request, COMMAND_STOP_TIMEOUT),
        )
        return RuntimeComponentLifecycle(endpoints, COMMAND_ENDPOINTS_PHASE)
    }

    private companion object {
        const val HEALTH_BEAN = "microserviceHealth"
        const val SPARK_BEAN = "microserviceSpark"
        const val SPARK_LIFECYCLE_BEAN = "microserviceSparkLifecycle"
        const val COMMAND_REGISTRATION_BEAN = "microserviceCommandRegistration"
        const val COMMAND_ENDPOINTS_BEAN = "microserviceCommandEndpoints"
        const val SPARK_PHASE = SurfApiStandaloneBootstrap.LIFECYCLE_PHASE - 1000
        const val COMMAND_ENDPOINTS_PHASE = SurfApiStandaloneBootstrap.LIFECYCLE_PHASE + 1000
        val COMMAND_STOP_TIMEOUT = 5.seconds
    }
}
