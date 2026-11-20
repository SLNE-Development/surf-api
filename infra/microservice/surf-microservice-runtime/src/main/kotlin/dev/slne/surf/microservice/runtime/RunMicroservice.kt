package dev.slne.surf.microservice.runtime

import dev.slne.surf.microservice.runtime.boot.MicroserviceContextInitializer
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceShutdown
import dev.slne.surf.microservice.runtime.logging.Slf4jProvider
import org.apache.logging.log4j.LogManager
import org.springframework.boot.Banner
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.logging.LoggingSystem
import kotlin.system.exitProcess

/**
 * Runs the Spring Boot application [A] as a surf microservice until the `exit` command, the close of its context or
 * a JVM shutdown signal stops it, then exits the process.
 *
 * The `main` function of the microservice calls it:
 *
 * ```
 * @SpringBootApplication
 * class PunishApplication
 *
 * fun main(args: Array<String>) {
 *     runMicroservice<PunishApplication>(args)
 * }
 * ```
 *
 * See the overload with a [Class] for the behavior.
 */
inline fun <reified A : Any> runMicroservice(args: Array<String>): Nothing {
    runMicroservice(A::class.java, args)
}

/**
 * Runs the Spring Boot application [application] as a surf microservice until the `exit` command, the close of its
 * context or a JVM shutdown signal stops it, then exits the process.
 *
 * The application context is surf-api's root context: surf-api's beans, the contributions of surf modules such as
 * config handles and NATS, and the application's own beans live in it, and the surf lifecycle hooks run when it
 * starts and stops. The application's package must lie outside `dev.slne.surf.api`.
 *
 * The environment variables that configure a running microservice:
 * - `SURF_MICROSERVICE_NAME`: the service name; otherwise `spring.application.name`, otherwise the class name of
 *   [application] without an `Application` or `Microservice` suffix, in kebab case.
 * - `SURF_MICROSERVICE_CONFIG_DIR`: the data folder, `config` by default.
 * - `SURF_MICROSERVICE_COMMAND_SOCKET`: the path of the command socket.
 * - `SURF_MICROSERVICE_SPARK_PROFILER`: `true` starts the spark profiler together with the microservice.
 *
 * A failed start exits with code `1`, the `exit` command with code `0`, even if a library left non-daemon threads
 * behind. Closing the context from a bean, with `SpringApplication.exit` or `close()`, ends the process too: with the
 * exit code that `SpringApplication.exit` computed, otherwise `0`. After a JVM shutdown signal, Spring Boot's shutdown
 * hook closes the context and the JVM exits with the signal's code.
 *
 * log4j shuts down last, in every case, once the close of the context is complete, even if another thread closes it:
 * Spring Boot leaves logging alone, and the bundled `log4j2.xml` disables log4j's own shutdown hook, which a
 * replacement must do as well.
 *
 * Nothing may log before [Slf4jProvider.selectLog4j] runs, so this file declares no logger property.
 *
 * @param args the command line arguments, available as Spring Boot's `ApplicationArguments`
 */
fun runMicroservice(application: Class<*>, args: Array<String>): Nothing {
    Slf4jProvider.selectLog4j()
    System.setProperty(LoggingSystem.SYSTEM_PROPERTY, LoggingSystem.NONE)
    SpringApplication.getShutdownHandlers().add(LogManager::shutdown)

    val shutdown = MicroserviceShutdown()
    val context = try {
        microserviceApplication(application, shutdown).run(*args)
    } catch (_: Throwable) {
        LogManager.shutdown()
        exitProcess(START_FAILURE_EXIT_CODE)
    }

    shutdown.awaitRequest()

    context.close()
    shutdown.awaitContextClosed()
    LogManager.shutdown()
    exitProcess(shutdown.exitCode)
}

/**
 * The Spring Boot application of a microservice: no web server, no banner, and surf-api's root context prepared
 * before anything else initializes it.
 */
internal fun microserviceApplication(application: Class<*>, shutdown: MicroserviceShutdown): SpringApplication {
    return SpringApplication(application).apply {
        setWebApplicationType(WebApplicationType.NONE)
        setBannerMode(Banner.Mode.OFF)
        setDefaultProperties(mapOf(HEALTH_DEFAULTS_PROPERTY to "false"))
        val initializer = MicroserviceContextInitializer(application, shutdown)
        addInitializers(initializer)
        addListeners(initializer)
    }
}

/**
 * Turns Spring Boot's own health indicators, such as disk space, off unless the application turns them on, so the
 * health check runs only the indicators of the application and of surf modules.
 */
private const val HEALTH_DEFAULTS_PROPERTY = "management.health.defaults.enabled"
private const val START_FAILURE_EXIT_CODE = 1
