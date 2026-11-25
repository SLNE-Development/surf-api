package dev.slne.surf.api.velocity.sharedbean.provider

import com.google.common.flogger.FluentLogger
import com.velocitypowered.api.plugin.Dependency
import com.velocitypowered.api.plugin.Plugin
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.api.velocity.plugin.SurfVelocityPlugin
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service

/**
 * The exported [SharedGreeting], which logs every phase of its context it goes through.
 */
@Service
class SharedGreetingImpl(private val internals: ProviderInternals) : SharedGreeting, SurfLifecycle,
    DisposableBean {

    private val log = logger()

    override fun greet(): String = SharedGreeting.GREETING

    override suspend fun onLoad(): Unit = logPhase(log, "load")
    override suspend fun onEnable(): Unit = logPhase(log, "enable")
    override suspend fun onDisable(): Unit = logPhase(log, "disable")
    override fun destroy(): Unit = logPhase(log, "destroy")

    companion object {

        /**
         * The prefix of the lines the provider logs, which the boot test reads.
         */
        const val PROVIDER_MARKER = "SURF-SPRING-SPIKE"
    }
}

/**
 * A bean that implements no `@PluginApi` interface and therefore must stay invisible to consumers.
 */
@Service
class ProviderInternals

/**
 * The provider's main class; its package is the root of the provider's component scan.
 *
 * It declares no constructor, like the main class the surf Gradle plugin generates.
 */
@Plugin(
    id = "surf-shared-bean-provider",
    name = "Surf Shared Bean Provider",
    version = "1.0.0",
    description = "Exports a @PluginApi bean for surf-api's Velocity boot test.",
    authors = ["SLNE Development Team"],
    dependencies = [Dependency(id = "surf-api-velocity")],
)
class SharedBeanProviderPlugin : SurfVelocityPlugin()

private fun logPhase(log: FluentLogger, name: String) {
    log.atInfo()
        .log("%s provider phase=%s", SharedGreetingImpl.PROVIDER_MARKER, name)
}
