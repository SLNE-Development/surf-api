package dev.slne.minestom.fixtures.beta

import dev.slne.minestom.fixtures.FixtureEvent
import dev.slne.minestom.fixtures.FixtureLog
import dev.slne.minestom.fixtures.alpha.AlphaGreeting
import dev.slne.minestom.fixtures.alpha.AlphaInternals
import dev.slne.surf.api.command.CommandRegistrar
import dev.slne.surf.api.event.SurfEventHandler
import dev.slne.surf.api.minestom.command.CommandAPICommand
import dev.slne.surf.api.minestom.command.dsl.anyExecutor
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import net.minestom.server.event.Event
import net.minestom.server.event.EventNode
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.ObjectProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/**
 * The consumer of the two-plugin fixture; it depends on `alpha`.
 */
class BetaPlugin : SurfMinestomPlugin() {
    companion object {
        var greetingFromAlpha: String? = null
        var alphaInternalsVisible: Boolean? = null
        var surfEventsHandled = 0
        var injectedLogger: Logger? = null
    }
}

/**
 * The logger `beta` declares itself; it wins over the plugin logger surf-api contributes.
 */
val BETA_OWN_LOGGER: Logger = LoggerFactory.getLogger("beta-own-logger")

/**
 * Records the slf4j [Logger] that `beta`'s context injects by type.
 */
@Component
class BetaLoggerProbe(logger: Logger) {
    init {
        BetaPlugin.injectedLogger = logger
    }
}

@Service
class BetaService(
    private val alpha: AlphaGreeting,
    private val alphaInternals: ObjectProvider<AlphaInternals>,
) : SurfLifecycle, DisposableBean {
    override suspend fun onLoad() {
        FixtureLog += "beta:load"
        BetaPlugin.greetingFromAlpha = alpha.greeting()
        BetaPlugin.alphaInternalsVisible = alphaInternals.ifAvailable != null
    }

    override suspend fun onEnable() {
        FixtureLog += "beta:enable"
    }

    override suspend fun onDisable() {
        FixtureLog += "beta:disable"
    }

    override fun destroy() {
        FixtureLog += "beta:destroy"
    }

    @SurfEventHandler
    fun onFixtureEvent(event: FixtureEvent) {
        BetaPlugin.surfEventsHandled++
    }
}

/**
 * Registers a command in the enable phase, while `beta` is the current plugin.
 */
@Component
class BetaCommands : CommandRegistrar {
    override fun register() {
        CommandAPICommand(BETA_COMMAND).anyExecutor { _, _ -> }.register()
    }
}

const val BETA_COMMAND = "beta-fixture-command"

@Configuration(proxyBeanMethods = false)
class BetaBeans {
    @Bean
    fun betaNode(): EventNode<Event> = EventNode.all("beta-listeners")

    @Bean
    fun betaOwnLogger(): Logger = BETA_OWN_LOGGER
}
