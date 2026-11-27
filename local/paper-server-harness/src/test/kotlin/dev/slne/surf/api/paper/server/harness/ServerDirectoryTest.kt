package dev.slne.surf.api.paper.server.harness

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ServerDirectoryTest {

    @TempDir
    lateinit var root: Path

    private val serverJar by lazy { file("input/paper.jar", "paper") }

    @Test
    fun `only the given plugins are installed`() {
        val plugin = file("input/probe.jar", "probe")
        file("server/plugins/left-behind.jar", "stale")

        val server = ServerDirectory.assemble(root.resolve("server"), serverJar, listOf(plugin))

        val installed = server.path.resolve("plugins").listDirectoryEntries("*.jar").map { it.name }
        assertEquals(listOf("probe.jar"), installed)
    }

    @Test
    fun `the server jar is the only jar in the server directory`() {
        file("server/other-server.jar", "other")

        val server = ServerDirectory.assemble(root.resolve("server"), serverJar, emptyList())

        val jars = server.path.listDirectoryEntries("*.jar").map { it.name }
        assertEquals(listOf("paper.jar"), jars)
        assertEquals("paper.jar", server.serverJarName)
    }

    @Test
    fun `the world survives a new assembly`() {
        val level = file("server/world/level.dat", "level")

        ServerDirectory.assemble(root.resolve("server"), serverJar, emptyList())

        assertTrue(level.exists())
    }

    @Test
    fun `a fresh world removes the previous one`() {
        val level = file("server/world/level.dat", "level")

        ServerDirectory.assemble(
            root.resolve("server"),
            serverJar,
            emptyList(),
            freshWorld = true,
        )

        assertFalse(level.parent.exists())
    }

    @Test
    fun `server properties carry the chosen port and the overrides`() {
        val server = ServerDirectory.assemble(
            root.resolve("server"),
            serverJar,
            emptyList(),
            properties = mapOf("max-players" to "20", "difficulty" to "peaceful"),
        )

        val properties = Properties()
        server.path.resolve("server.properties").reader().use { properties.load(it) }

        assertEquals(server.port.toString(), properties.getProperty("server-port"))
        assertEquals("20", properties.getProperty("max-players"))
        assertEquals("peaceful", properties.getProperty("difficulty"))
        assertEquals("false", properties.getProperty("online-mode"))
    }

    private fun file(relative: String, content: String): Path {
        val path = root.resolve(relative)
        path.parent.createDirectories()
        path.writeText(content)

        return path
    }
}
