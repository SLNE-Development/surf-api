package dev.slne.minestom.example.plugin

import dev.slne.surf.api.spring.SurfLifecycle
import org.springframework.stereotype.Service
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText

/**
 * The greeting shown to joining players, read from `greeting.txt` in the plugin's data directory.
 */
@Service
class Greeting(private val plugin: ExamplePlugin) : SurfLifecycle {

    private lateinit var loaded: String

    /**
     * The greeting; available once the plugin is loaded.
     */
    val text: String get() = loaded

    override suspend fun onLoad() {
        val file = plugin.dataDirectory.createDirectories().resolve("greeting.txt")

        if (!file.exists()) {
            file.writeText("Welcome to the surf api example server!")
        }

        loaded = file.readText().trim()

        plugin.logger.info("Loaded the greeting from {}", file)
    }
}
