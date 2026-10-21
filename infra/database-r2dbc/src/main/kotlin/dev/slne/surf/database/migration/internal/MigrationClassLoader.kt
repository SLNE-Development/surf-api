package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.DatabaseApi
import java.net.URL
import java.util.Collections
import java.util.Enumeration

/**
 * Class loader handed to Flyway for one migration run.
 *
 * Classes are loaded from the library's class loader first, so Flyway's `ServiceLoader` discovers the (relocated)
 * Flyway plugins bundled with this library. Resources are looked up in the library and in the [plugin] class loader,
 * so Flyway's classpath scan also finds the plugin's SQL files. Classes the library cannot load fall back to the
 * [plugin] class loader.
 */
internal class MigrationClassLoader(
    private val plugin: ClassLoader
) : ClassLoader(DatabaseApi::class.java.classLoader) {

    override fun findClass(name: String): Class<*> = plugin.loadClass(name)

    override fun getResource(name: String): URL? = parent.getResource(name) ?: plugin.getResource(name)

    /**
     * Returns the parent's resources followed by the plugin's, without duplicates (the plugin may delegate to the
     * same loader, which would otherwise make Flyway see every SQL file twice).
     */
    override fun getResources(name: String): Enumeration<URL> {
        val urls = LinkedHashMap<String, URL>()
        for (url in parent.getResources(name)) urls.putIfAbsent(url.toExternalForm(), url)
        if (plugin !== parent) {
            for (url in plugin.getResources(name)) urls.putIfAbsent(url.toExternalForm(), url)
        }
        return Collections.enumeration(urls.values)
    }
}
