package dev.slne.surf.api.redis

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class RedisConfigLocationTest {

    @TempDir
    lateinit var plugins: Path

    private val surfApi get() = plugins.resolve("surf-api")

    @Test
    fun `the configuration folder is the redis folder of surf-api's data folder`() {
        assertEquals(surfApi.resolve("redis"), RedisConfigLocation.resolve(surfApi))
    }

    @Test
    fun `a legacy configuration is copied once and left in place`() {
        writeConfig(plugins.resolve("surf-api-redis-paper"), "host: legacy")

        val folder = RedisConfigLocation.resolve(surfApi)

        assertEquals("host: legacy", folder.resolve("config.yml").readText())
        assertEquals("host: legacy", plugins.resolve("surf-api-redis-paper/config.yml").readText())
    }

    @Test
    fun `the configuration of the retired Minestom plugin is copied`() {
        for (legacy in listOf("surf-api-redis-minestom", "surf-redis-minestom")) {
            val pluginsFolder = plugins.resolve("for-$legacy")
            writeConfig(pluginsFolder.resolve(legacy), "host: $legacy")

            val folder = RedisConfigLocation.resolve(pluginsFolder.resolve("surf-api"))

            assertEquals("host: $legacy", folder.resolve("config.yml").readText())
        }
    }

    @Test
    fun `an existing configuration is never overwritten`() {
        writeConfig(plugins.resolve("surf-redis-velocity"), "host: legacy")
        writeConfig(surfApi.resolve("redis"), "host: current")

        val folder = RedisConfigLocation.resolve(surfApi)

        assertEquals("host: current", folder.resolve("config.yml").readText())
    }

    @Test
    fun `without a legacy configuration nothing is created`() {
        val folder = RedisConfigLocation.resolve(surfApi)

        assertFalse(folder.exists())
    }

    private fun writeConfig(folder: Path, content: String) {
        folder.createDirectories()
        folder.resolve("config.yml").writeText(content)
    }
}
