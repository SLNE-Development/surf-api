package dev.slne.minestom.fixtures.alpha

import dev.slne.minestom.fixtures.FixtureLog
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.PluginApi
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service

/**
 * The provider of the two-plugin fixture; `beta` depends on it.
 */
class AlphaPlugin : SurfMinestomPlugin()

/**
 * What `alpha` exports to the plugins that depend on it.
 */
@PluginApi
interface AlphaGreeting {
    fun greeting(): String
}

@Service
class AlphaGreetingService : AlphaGreeting, SurfLifecycle, DisposableBean {
    override fun greeting() = "hello from alpha"

    override suspend fun onLoad() {
        FixtureLog += "alpha:load"
    }

    override suspend fun onEnable() {
        FixtureLog += "alpha:enable"
    }

    override suspend fun onDisable() {
        FixtureLog += "alpha:disable"
    }

    override fun destroy() {
        FixtureLog += "alpha:destroy"
    }
}

/**
 * A bean of `alpha` that is not part of its api, so `beta` must not see it.
 */
@Component
class AlphaInternals
