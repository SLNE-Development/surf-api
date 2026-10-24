package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.PluginDependency
import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.PluginMeta
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.plugins.impl.PluginCandidate
import dev.slne.surf.api.minestom.server.plugins.impl.PluginDiscovery
import dev.slne.surf.api.minestom.server.plugins.impl.PluginManagerImpl
import net.minestom.server.event.EventNode
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.lang.reflect.Proxy
import java.net.URLClassLoader
import java.nio.file.Path
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.io.path.*

class PluginsRequireAllTest {

    /**
     * A [SurfMinestomServer] that only provides the event node, which is all the plugins feature
     * uses of it.
     */
    private fun fakeServer(): SurfMinestomServer {
        val eventNode = EventNode.all("test")
        return Proxy.newProxyInstance(
            SurfMinestomServer::class.java.classLoader,
            arrayOf(SurfMinestomServer::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getEventNode" -> eventNode
                else -> throw UnsupportedOperationException(method.name)
            }
        } as SurfMinestomServer
    }

    /**
     * Writes a plugin jar that only carries its descriptor; the main class comes from the test class
     * path through the parent class loader.
     */
    private fun writePlugin(directory: Path, id: String, main: Class<*>, dependencies: String = "") =
        writeJar(
            directory / "$id.jar",
            PluginMeta.FILE_NAME,
            """{ "id": "$id", "main": "${main.name}", "version": "1.0", "dependencies": [$dependencies] }""",
        )

    private fun writeJar(jar: Path, entry: String, content: String) {
        JarOutputStream(jar.outputStream()).use { out ->
            out.putNextEntry(JarEntry(entry))
            out.write(content.toByteArray())
            out.closeEntry()
        }
    }

    private fun feature(directory: Path, requireAll: Boolean) = PluginsFeature(
        PluginsFeatureBuilder().apply {
            this.directory = directory
            loadClassPathPlugins = false
            this.requireAll = requireAll
        }
    )

    private fun <T> withFeature(directory: Path, requireAll: Boolean, block: (PluginsFeature, SurfMinestomServer) -> T): T {
        val api = fakeServer()
        val feature = feature(directory, requireAll)
        try {
            return block(feature, api)
        } finally {
            feature.disable(api)
        }
    }

    @Test
    fun `requireAll fails the load when a plugin throws from onLoad`(@TempDir directory: Path) {
        RecordingPlugin.events.clear()
        writePlugin(directory, "broken", FailingLoadPlugin::class.java)
        writePlugin(directory, "healthy", RecordingPlugin::class.java)

        withFeature(directory, requireAll = true) { feature, api ->
            val failure = assertThrows(IllegalStateException::class.java) { feature.load(api) }
            assertTrue("broken" in failure.message.orEmpty(), failure.message)
            assertFalse("healthy" in failure.message.orEmpty(), failure.message)
        }

        assertEquals(listOf("load", "disable"), RecordingPlugin.events, "the loaded plugins are rolled back")
        assertThrows(IllegalStateException::class.java) { PluginManager.instance }
    }

    @Test
    fun `requireAll fails the load when a required dependency is missing`(@TempDir directory: Path) {
        writePlugin(directory, "orphan", RecordingPlugin::class.java, """{ "id": "absent" }""")

        withFeature(directory, requireAll = true) { feature, api ->
            val failure = assertThrows(IllegalStateException::class.java) { feature.load(api) }
            assertTrue("orphan" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `requireAll fails the load when a required dependency failed`(@TempDir directory: Path) {
        writePlugin(directory, "broken", FailingLoadPlugin::class.java)
        writePlugin(directory, "dependent", RecordingPlugin::class.java, """{ "id": "broken" }""")

        withFeature(directory, requireAll = true) { feature, api ->
            val failure = assertThrows(IllegalStateException::class.java) { feature.load(api) }
            assertTrue("broken" in failure.message.orEmpty(), failure.message)
            assertTrue("dependent" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `requireAll fails the load when a plugin description cannot be read`(@TempDir directory: Path) {
        writeJar(directory / "unreadable.jar", PluginMeta.FILE_NAME, """{ "id": """)
        writePlugin(directory, "healthy", RecordingPlugin::class.java)

        withFeature(directory, requireAll = true) { feature, api ->
            val failure = assertThrows(IllegalStateException::class.java) { feature.load(api) }
            assertTrue("unreadable.jar" in failure.message.orEmpty(), failure.message)
            assertFalse("healthy" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `a jar without a plugin description does not count for requireAll`(@TempDir directory: Path) {
        writeJar(directory / "library.jar", "library.txt", "not a plugin")
        writePlugin(directory, "healthy", RecordingPlugin::class.java)

        withFeature(directory, requireAll = true) { feature, api ->
            feature.load(api)
            assertEquals(listOf("healthy"), PluginManager.instance.plugins.map { it.meta.id })
        }
    }

    @Test
    fun `discovery reports a class path plugin whose description is missing`(@TempDir directory: Path) {
        // A directory instead of a jar, whose cached handle would keep the temporary directory on Windows
        (directory / PluginMeta.CLASSPATH_INDEX).createParentDirectories().writeText("com.example.MissingPlugin\n")

        val failures = linkedMapOf<String, String>()
        val candidates = URLClassLoader(arrayOf(directory.toUri().toURL()), null).use { loader ->
            PluginDiscovery.fromClassPath(loader, failures)
        }

        assertEquals(emptyList<PluginCandidate>(), candidates)
        assertEquals(setOf("com.example.MissingPlugin"), failures.keys)
    }

    @Test
    fun `requireAll fails the enable when a plugin throws from onEnable`(@TempDir directory: Path) {
        writePlugin(directory, "broken", FailingEnablePlugin::class.java)
        writePlugin(directory, "healthy", RecordingPlugin::class.java)

        withFeature(directory, requireAll = true) { feature, api ->
            feature.load(api)

            val failure = assertThrows(IllegalStateException::class.java) { feature.enable(api) }
            assertTrue("broken" in failure.message.orEmpty(), failure.message)
            assertFalse("healthy" in failure.message.orEmpty(), failure.message)
        }
    }

    @Test
    fun `plugins that fail to load or enable are skipped by default`(@TempDir directory: Path) {
        RecordingPlugin.events.clear()
        writePlugin(directory, "broken-load", FailingLoadPlugin::class.java)
        writePlugin(directory, "broken-enable", FailingEnablePlugin::class.java)
        writePlugin(directory, "orphan", RecordingPlugin::class.java, """{ "id": "absent" }""")
        writePlugin(directory, "healthy", RecordingPlugin::class.java)

        withFeature(directory, requireAll = false) { feature, api ->
            feature.load(api)
            feature.enable(api)

            val manager = PluginManager.instance
            assertEquals(listOf("healthy"), manager.plugins.map { it.meta.id })
            assertEquals(listOf("load", "enable"), RecordingPlugin.events)
        }
    }

    @Test
    fun `the manager reports every plugin that failed with its reason`(@TempDir directory: Path) {
        fun candidate(id: String, main: Class<*>, vararg dependencies: String) = PluginCandidate(
            PluginMeta(
                id,
                main.name,
                dependencies = dependencies.map { PluginDependency(it) },
            ),
            jar = null,
        )

        val manager = PluginManagerImpl(directory, EventNode.all("test"), PluginsRequireAllTest::class.java.classLoader)
        try {
            val loadFailures = manager.load(
                listOf(
                    candidate("broken-load", FailingLoadPlugin::class.java),
                    candidate("dependent", RecordingPlugin::class.java, "broken-load"),
                    candidate("orphan", RecordingPlugin::class.java, "absent"),
                    candidate("broken-enable", FailingEnablePlugin::class.java),
                    candidate("healthy", RecordingPlugin::class.java),
                )
            )

            assertEquals(setOf("broken-load", "dependent", "orphan"), loadFailures.keys)
            assertTrue("onLoad fails on purpose" in loadFailures.getValue("broken-load"))
            assertTrue("broken-load" in loadFailures.getValue("dependent"))

            val enableFailures = manager.enable()
            assertEquals(setOf("broken-enable"), enableFailures.keys)
            assertTrue("onEnable fails on purpose" in enableFailures.getValue("broken-enable"))
        } finally {
            manager.disableAll()
        }
    }

    @Test
    fun `the data directory exists before the plugin is constructed`(@TempDir directory: Path) {
        DataDirectoryPlugin.constructionSawDirectory = null
        DataDirectoryPlugin.loadSawDirectory = null
        writePlugin(directory, "data-plugin", DataDirectoryPlugin::class.java)

        withFeature(directory, requireAll = true) { feature, api ->
            feature.load(api)

            assertEquals(true, DataDirectoryPlugin.constructionSawDirectory)
            assertEquals(true, DataDirectoryPlugin.loadSawDirectory)
            assertTrue((directory / "data-plugin").isDirectory())
        }
    }
}
