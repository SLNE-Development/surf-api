package dev.slne.surf.api.paper.sharedbean.provider

import com.google.common.flogger.FluentLogger
import dev.slne.surf.api.common.util.logger
import dev.slne.surf.api.spring.BootstrapComponent
import dev.slne.surf.api.spring.SurfBootstrapLifecycle
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service

/**
 * The exported [SharedGreeting], which logs every phase of the full context it goes through.
 */
@Service
class SharedGreetingImpl(private val internals: ProviderInternals) : SharedGreeting, SurfLifecycle, DisposableBean {

    private val log = logger()

    override fun greet() = SharedGreeting.GREETING

    override suspend fun onLoad() = logPhase(log, "load")
    override suspend fun onEnable() = logPhase(log, "enable")
    override suspend fun onDisable() = logPhase(log, "disable")
    override fun destroy() = logPhase(log, "destroy")

    companion object {

        /**
         * The prefix of the lines the provider logs, which the boot test reads.
         */
        const val PROVIDER_MARKER = "SURF-SPRING-SPIKE"
    }
}

/**
 * The provider's bootstrap bean. It depends on a [BootstrapComponent], which therefore lives in the
 * bootstrap context with it.
 */
@Service
class ProviderBootstrapProbe(private val helper: ProviderBootstrapHelper) : SurfBootstrapLifecycle {

    private val log = logger()

    override suspend fun onBootstrap() = logPhase(log, "bootstrap:${helper.answer}")
}

/**
 * A helper a bootstrap bean needs.
 */
@BootstrapComponent
class ProviderBootstrapHelper {
    val answer = "helper"
}

/**
 * A bean that implements no `@PluginApi` interface and therefore must stay invisible to consumers.
 */
@Service
class ProviderInternals

private fun logPhase(log: FluentLogger, name: String) {
    log.atInfo().log("%s provider phase=%s", SharedGreetingImpl.PROVIDER_MARKER, name)
}
