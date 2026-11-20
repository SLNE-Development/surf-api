package dev.slne.minestom.fixtures.recording

import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Component

/**
 * A plugin that records its lifecycle calls and never fails.
 *
 * Every plugin that uses this class as its main class records into the same [events].
 */
class RecordingPlugin : SurfMinestomPlugin() {
    companion object {
        val events = mutableListOf<String>()
    }
}

@Component
class RecordingLifecycle : SurfLifecycle {
    override suspend fun onLoad() {
        RecordingPlugin.events += "load"
    }

    override suspend fun onEnable() {
        RecordingPlugin.events += "enable"
    }

    override suspend fun onDisable() {
        RecordingPlugin.events += "disable"
    }
}
