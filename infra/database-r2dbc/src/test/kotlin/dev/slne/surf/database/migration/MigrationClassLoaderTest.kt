package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.internal.MigrationClassLoader
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.net.URL
import java.util.Collections
import java.util.Enumeration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

class MigrationClassLoaderTest {

    private val library: ClassLoader = DatabaseApi::class.java.classLoader

    /**
     * A plugin class loader without parent that only knows [resources] and [classes].
     */
    private class PluginLoader(
        private val resources: Map<String, List<URL>> = emptyMap(),
        private val classes: Map<String, Class<*>> = emptyMap()
    ) : ClassLoader(null) {
        override fun findClass(name: String): Class<*> = classes[name] ?: throw ClassNotFoundException(name)
        override fun findResource(name: String): URL? = resources[name]?.firstOrNull()
        override fun findResources(name: String): Enumeration<URL> =
            Collections.enumeration(resources[name].orEmpty())
    }

    private fun file(dir: File, name: String): URL = File(dir, name).apply { writeText("SELECT 1;") }.toURI().toURL()

    @Test
    fun parentIsTheLibraryClassLoader() {
        assertSame(library, MigrationClassLoader(PluginLoader()).parent)
    }

    @Test
    fun loadsLibraryClassesFromTheLibrary() {
        val loader = MigrationClassLoader(PluginLoader(classes = mapOf(Flyway::class.java.name to String::class.java)))

        assertSame(Flyway::class.java, loader.loadClass(Flyway::class.java.name))
    }

    @Test
    fun fallsBackToThePluginForUnknownClasses() {
        val loader = MigrationClassLoader(PluginLoader(classes = mapOf("plugin.Only" to String::class.java)))

        assertSame(String::class.java, loader.loadClass("plugin.Only"))
    }

    @Test
    fun unknownClassIsNotFound() {
        val loader = MigrationClassLoader(PluginLoader())

        assertFailsWith<ClassNotFoundException> { loader.loadClass("plugin.Missing") }
    }

    @Test
    fun findsResourcesOfTheLibrary() {
        val loader = MigrationClassLoader(PluginLoader())

        assertEquals(library.getResource("migrations/plugin_a"), loader.getResource("migrations/plugin_a"))
    }

    @Test
    fun findsResourcesOfThePlugin(@TempDir dir: File) {
        val url = file(dir, "V1__plugin.sql")
        val loader = MigrationClassLoader(PluginLoader(resources = mapOf("plugin/V1__plugin.sql" to listOf(url))))

        assertEquals(url, loader.getResource("plugin/V1__plugin.sql"))
        assertEquals(listOf(url), loader.getResources("plugin/V1__plugin.sql").toList())
    }

    @Test
    fun missingResourceIsNull() {
        assertNull(MigrationClassLoader(PluginLoader()).getResource("does/not/exist.sql"))
    }

    @Test
    fun prefersTheLibraryResourceOverThePlugin(@TempDir dir: File) {
        val pluginUrl = file(dir, "V1__init.sql")
        val name = "migrations/plugin_a/V1__init.sql"
        val loader = MigrationClassLoader(PluginLoader(resources = mapOf(name to listOf(pluginUrl))))

        assertEquals(assertNotNull(library.getResource(name)), loader.getResource(name))
    }

    @Test
    fun listsLibraryResourcesBeforePluginResources(@TempDir dir: File) {
        val pluginUrl = file(dir, "V1__init.sql")
        val name = "migrations/plugin_a/V1__init.sql"
        val loader = MigrationClassLoader(PluginLoader(resources = mapOf(name to listOf(pluginUrl))))

        assertEquals(listOf(assertNotNull(library.getResource(name)), pluginUrl), loader.getResources(name).toList())
    }

    @Test
    fun pluginDelegatingToTheLibraryDoesNotDuplicateResources() {
        val delegating = object : ClassLoader(library) {}

        val urls = MigrationClassLoader(delegating).getResources("migrations/plugin_a").toList()

        assertEquals(library.getResources("migrations/plugin_a").toList(), urls)
        assertEquals(1, urls.size)
    }

    @Test
    fun pluginThatIsTheLibraryDoesNotDuplicateResources() {
        val urls = MigrationClassLoader(library).getResources("migrations/plugin_a").toList()

        assertEquals(1, urls.size)
    }
}
