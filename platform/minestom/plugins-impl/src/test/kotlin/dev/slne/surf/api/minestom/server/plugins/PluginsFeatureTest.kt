package dev.slne.surf.api.minestom.server.plugins

import dev.slne.minestom.fixtures.FixtureEvent
import dev.slne.minestom.fixtures.test.BEAN_COMMAND
import dev.slne.minestom.fixtures.test.REGISTRAR_COMMAND
import dev.slne.minestom.fixtures.test.TestPlugin
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
        TestPlugin.seenAtEnable = null
        TestPlugin.surfEventsHandled = 0
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
            assertEquals(
                listOf("load", "enable"),
                TestPlugin.calls,
                "the plugin was not enabled before the start returned",
            )
            assertEquals(
                TestPlugin.SeenAtEnable(
                    eventNodeAttached = true,
                    registrarCommand = true,
                    beanCommand = true,
                ),
                TestPlugin.seenAtEnable,
                "listeners and commands are registered before the enable hooks run",
            )
            assertTrue(commands.commandExists(REGISTRAR_COMMAND))
            assertTrue(commands.commandExists(BEAN_COMMAND))
            assertTrue(plugin.eventNode.parent != null)

            FixtureEvent().call()
            assertEquals(
                1,
                TestPlugin.surfEventsHandled,
                "the @SurfEventHandler bean is registered",
            )
            plugin
        } finally {
            server.shutdown()
        }

        assertEquals(listOf("load", "enable", "disable"), TestPlugin.calls)
        assertFalse(
            commands.commandExists(REGISTRAR_COMMAND),
            "the plugin's registrar command is unregistered",
        )
        assertFalse(
            commands.commandExists(BEAN_COMMAND),
            "the plugin's bean command is unregistered",
        )
        assertNull(plugin.eventNode.parent, "the plugin's event node is removed")
    }

    @Test
    fun `a jar is read through its minestom-plugin json`(@TempDir directory: Path) {
        val plugin = directory.resolve("example.jar")
        JarOutputStream(plugin.outputStream()).use { jar ->
            jar.putNextEntry(JarEntry(PluginMeta.FILE_NAME))
            jar.write(
                """{ "id": "example", "main": "example.Main", "version": "2.0" }""".toByteArray(),
            )
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
