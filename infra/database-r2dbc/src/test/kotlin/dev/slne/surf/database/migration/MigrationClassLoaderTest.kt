package dev.slne.surf.database.migration

import dev.slne.surf.database.DatabaseApi
import dev.slne.surf.database.migration.internal.MigrationClassLoader
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import java.net.URI
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

    private val pluginUrl = URI("file:/plugin/V1__init.sql").toURL()

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
    fun findsResourcesOfThePlugin() {
        val loader = MigrationClassLoader(PluginLoader(resources = mapOf("plugin/V1__init.sql" to listOf(pluginUrl))))

        assertEquals(pluginUrl, loader.getResource("plugin/V1__init.sql"))
        assertEquals(listOf(pluginUrl), loader.getResources("plugin/V1__init.sql").toList())
    }

    @Test
    fun missingResourceIsNull() {
        assertNull(MigrationClassLoader(PluginLoader()).getResource("does/not/exist.sql"))
    }

    @Test
    fun libraryResourcesComeBeforePluginResources() {
        val name = "migrations/plugin_a/V1__init.sql"
        val libraryUrl = assertNotNull(library.getResource(name))
        val loader = MigrationClassLoader(PluginLoader(resources = mapOf(name to listOf(pluginUrl))))

        assertEquals(libraryUrl, loader.getResource(name))
        assertEquals(listOf(libraryUrl, pluginUrl), loader.getResources(name).toList())
    }

    @Test
    fun pluginSharingTheLibraryResourcesDoesNotDuplicateThem() {
        val expected = library.getResources("migrations/plugin_a").toList()

        listOf(object : ClassLoader(library) {}, library).forEach { plugin ->
            assertEquals(expected, MigrationClassLoader(plugin).getResources("migrations/plugin_a").toList())
        }
        assertEquals(1, expected.size)
    }
}
