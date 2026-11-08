package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.PluginMeta
import dev.slne.surf.api.minestom.server.plugins.impl.PluginDiscovery
import dev.slne.surf.api.minestom.server.surfMinestomServer
import net.minestom.testing.Env
import net.minestom.testing.EnvTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.outputStream

@EnvTest
class PluginsFeatureTest {

    @Test
    fun `a plugin on the class path is loaded and disabled with its commands and listeners`(
        env: Env,
        @TempDir directory: Path,
    ) {
        TestPlugin.calls.clear()
        val commands = env.process().command()

        val server = surfMinestomServer {
            withPlugins { this.directory = directory }
        }

        val plugin = try {
            val plugin = PluginManager.instance.plugin("test-plugin")
            assertNotNull(plugin)
            assertSame(TestPlugin.instance, plugin)
            assertEquals("TestPlugin", plugin!!.meta.name)
            assertEquals(directory.resolve("test-plugin"), plugin.dataDirectory)
            assertEquals(listOf("load", "enable"), TestPlugin.calls, "the plugin was not enabled before the start returned")
            assertTrue(commands.commandExists("test-plugin-command"))
            assertTrue(plugin.eventNode.parent != null)
            plugin
        } finally {
            server.shutdown()
        }

        assertEquals(listOf("load", "enable", "disable"), TestPlugin.calls)
        assertFalse(commands.commandExists("test-plugin-command"), "the plugin's command is unregistered")
        assertNull(plugin.eventNode.parent, "the plugin's event node is removed")
    }

    @Test
    fun `a jar is read through its minestom-plugin json`(@TempDir directory: Path) {
        val plugin = directory.resolve("example.jar")
        JarOutputStream(plugin.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry(PluginMeta.FILE_NAME))
            jar.write("""{ "id": "example", "main": "example.Main", "version": "2.0" }""".toByteArray())
            jar.closeEntry()
        }

        val other = directory.resolve("library.jar")
        JarOutputStream(other.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry("library.txt"))
            jar.closeEntry()
        }

        val candidates = PluginDiscovery.fromDirectory(directory)
        assertEquals(1, candidates.size, "a jar without a plugin file is no plugin")
        assertEquals("example", candidates.single().meta.id)
        assertEquals("2.0", candidates.single().meta.version)
        assertEquals(plugin, candidates.single().jar)
    }
}
