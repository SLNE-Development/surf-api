package dev.slne.minestom.fixtures.failingload

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Component

/**
 * A plugin whose `onLoad` hook throws.
 */
class FailingLoadPlugin : SurfMinestomPlugin()

@Component
class FailingLoadLifecycle : SurfLifecycle {
    override suspend fun onLoad() {
        error("onLoad fails on purpose")
    }
}
