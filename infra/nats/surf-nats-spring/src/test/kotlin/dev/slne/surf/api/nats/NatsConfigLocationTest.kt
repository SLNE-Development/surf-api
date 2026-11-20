package dev.slne.surf.api.nats

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class NatsConfigLocationTest {

    @TempDir
    lateinit var plugins: Path

    private val surfApi get() = plugins.resolve("surf-api")

    @Test
    fun `the configuration folder is the nats folder of the surf-api data folder`() {
        assertEquals(surfApi.resolve("nats"), NatsConfigLocation.resolve(surfApi))
    }

    @Test
    fun `a legacy configuration is copied once and left in place`() {
        writeConfig(plugins.resolve("surf-api-nats-paper"), "servers: legacy")

        val folder = NatsConfigLocation.resolve(surfApi)

        assertEquals("servers: legacy", folder.resolve("config.yml").readText())
        assertEquals("servers: legacy", plugins.resolve("surf-api-nats-paper/config.yml").readText())
    }

    @Test
    fun `the Minestom legacy folder is found too`() {
        writeConfig(plugins.resolve("surf-nats-minestom"), "servers: minestom")

        assertEquals("servers: minestom", NatsConfigLocation.resolve(surfApi).resolve("config.yml").readText())
    }

    @Test
    fun `an existing configuration is never overwritten`() {
        writeConfig(plugins.resolve("surf-nats-velocity"), "servers: legacy")
        writeConfig(surfApi.resolve("nats"), "servers: current")

        val folder = NatsConfigLocation.resolve(surfApi)

        assertEquals("servers: current", folder.resolve("config.yml").readText())
    }

    @Test
    fun `without a legacy configuration nothing is created`() {
        val folder = NatsConfigLocation.resolve(surfApi)

        assertFalse(folder.exists())
    }

    private fun writeConfig(folder: Path, content: String) {
        folder.createDirectories()
        folder.resolve("config.yml").writeText(content)
    }
}
