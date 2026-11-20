package dev.slne.minestom.fixtures.failingenable

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Component

/**
 * A plugin whose `onEnable` hook throws.
 */
class FailingEnablePlugin : SurfMinestomPlugin()

@Component
class FailingEnableLifecycle : SurfLifecycle {
    override suspend fun onEnable() {
        error("onEnable fails on purpose")
    }
}
