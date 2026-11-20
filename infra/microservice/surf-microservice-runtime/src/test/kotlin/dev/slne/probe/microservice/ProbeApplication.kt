package dev.slne.probe.microservice

import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.event.SurfSyncEvent
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.spring.context.ContextPhaseListener
import dev.slne.surf.microservice.api.MicroserviceInfo
import dev.slne.surf.microservice.runtime.health.MicroserviceHealth
import dev.slne.surf.microservice.runtime.lifecycle.MicroserviceShutdown
import dev.slne.surf.microservice.runtime.runMicroservice
import org.springframework.boot.ExitCodeGenerator
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Component
import org.apache.logging.log4j.LogManager
import kotlin.concurrent.thread

/**
 * A microservice that prints its lifecycle and asks to be shut down once it is enabled.
 *
 * With [FAIL_ENABLE] as an argument, its `onEnable` hook fails instead. With [EXIT_CONTEXT] it exits through
 * `SpringApplication.exit` with [CONTEXT_EXIT_CODE] instead of asking for a shutdown. With [LATE_DISABLE_LOG], its
 * `onDisable` hook sleeps before it logs through log4j, as the slow shutdown of a real service would.
 */
@SpringBootApplication
class ProbeApplication {

    @Bean
    fun probeHealth() = HealthIndicator { Health.up().build() }
}

@Component
internal class ProbeHooks(
    private val info: MicroserviceInfo,
    private val health: MicroserviceHealth,
    private val shutdown: MicroserviceShutdown,
    private val context: ConfigurableApplicationContext,
) : SurfLifecycle, ContextPhaseListener {

    override suspend fun onEnable() {
        println("probe:onEnable name=${info.name}")
        ProbeEvent("onEnable").call()
        check(System.getProperty(FAIL_ENABLE) == null) { "enable failed" }
    }

    override suspend fun afterEnable() {
        val results = health.checkAll()
        println("probe:afterEnable health=${results.joinToString { "${it.displayName}:${it.healthy}" }}")
        if (System.getProperty(EXIT_CONTEXT) == null) {
            shutdown.request()
        } else {
            // From another thread: the refresh thread holds the context's lock until this hook returns.
            thread(name = "probe-exit") { SpringApplication.exit(context, ExitCodeGenerator { CONTEXT_EXIT_CODE }) }
        }
    }

    override suspend fun beforeDisable() {
        println("probe:beforeDisable")
    }

    override suspend fun onDisable() {
        if (System.getProperty(LATE_DISABLE_LOG) != null) {
            // Obtained before the delay: a logger of a log4j context that was shut down meanwhile drops the message.
            val logger = LogManager.getLogger("probe")
            Thread.sleep(LATE_LOG_DELAY_MILLIS)
            logger.info(LATE_LOG_MESSAGE)
        }
        println("probe:onDisable")
    }
}

data class ProbeEvent(val source: String) : SurfSyncEvent()

/**
 * Prints every [ProbeEvent] it receives; surf-api registers it as a `@SurfEventHandler` bean.
 */
@Component
internal class ProbeEventListener {

    @SurfEventHandler
    fun onProbeEvent(event: ProbeEvent) {
        println("probe:event source=${event.source}")
    }
}

const val FAIL_ENABLE = "--fail-enable"
const val EXIT_CONTEXT = "--exit-context"
const val LATE_DISABLE_LOG = "--late-disable-log"
const val LATE_LOG_MESSAGE = "probe:late-log"
const val CONTEXT_EXIT_CODE = 7
private const val LATE_LOG_DELAY_MILLIS = 1500L

fun main(args: Array<String>) {
    if (FAIL_ENABLE in args) System.setProperty(FAIL_ENABLE, "true")
    if (EXIT_CONTEXT in args) System.setProperty(EXIT_CONTEXT, "true")
    if (LATE_DISABLE_LOG in args) System.setProperty(LATE_DISABLE_LOG, "true")
    runMicroservice<ProbeApplication>(args)
}
