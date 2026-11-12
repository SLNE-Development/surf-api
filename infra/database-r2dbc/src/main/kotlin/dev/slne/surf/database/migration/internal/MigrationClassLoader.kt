package dev.slne.surf.database.migration.internal

import dev.slne.surf.database.DatabaseApi
import java.net.URL
import java.util.*

/**
 * Class loader that Flyway runs with during a migration.
 *
 * Classes and resources are looked up in the library first and in [plugin] second. The library comes first so that
 * Flyway's `ServiceLoader` finds the relocated Flyway plugins bundled with this library.
 *
 * Resources under one of the [sqlPaths] come from [plugin] only, so SQL files that the library's class loader
 * happens to see under the same path are never run as migrations of the plugin.
 *
 * @param sqlPaths Classpath paths of the SQL locations, such as `db/migrations`.
 */
internal class MigrationClassLoader(
    private val plugin: ClassLoader,
    private val sqlPaths: List<String>,
) : ClassLoader(DatabaseApi::class.java.classLoader) {

    override fun findClass(name: String): Class<*> = plugin.loadClass(name)

    override fun getResource(name: String): URL? {
        if (isSqlResource(name)) return plugin.getResource(name)
        return parent.getResource(name) ?: plugin.getResource(name)
    }

    /**
     * Returns the library's resources followed by the plugin's.
     *
     * A URL found by both, for example when [plugin] delegates to the library, is returned once. URLs are compared by
     * their external form because [URL.equals] resolves hosts.
     */
    override fun getResources(name: String): Enumeration<URL> {
        if (isSqlResource(name)) return plugin.getResources(name)

        val urls = (parent.getResources(name).asSequence() + plugin.getResources(name).asSequence())
            .distinctBy { it.toExternalForm() }
            .toList()

        return Collections.enumeration(urls)
    }

    private fun isSqlResource(name: String): Boolean {
        val path = name.trimStart('/')
        return sqlPaths.any { root -> root.isEmpty() || path == root || path.startsWith("$root/") }
    }
}
