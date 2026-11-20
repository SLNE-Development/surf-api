package dev.slne.minestom.example.plugin

import dev.slne.surf.api.spring.SurfLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.springframework.stereotype.Service
import kotlin.time.Duration.Companion.seconds

/**
 * Logs a line every five minutes while the plugin runs, from a coroutine in the plugin's scope.
 */
@Service
class Heartbeat(private val plugin: ExamplePlugin) : SurfLifecycle {

    override suspend fun onEnable() {
        plugin.scope.launch {
            while (true) {
                delay(300.seconds)
                plugin.logger.info("{} is still running", plugin.meta.name)
            }
        }
    }

    override suspend fun onDisable() {
        plugin.logger.info("Goodbye from {}", plugin.meta.name)
    }
}
