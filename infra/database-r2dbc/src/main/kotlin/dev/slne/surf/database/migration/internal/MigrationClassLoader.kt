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
 * Resources under one of the [sqlLocations] come from [plugin] only, so SQL files that the library's class loader
 * happens to see under the same path are never run as migrations of the plugin.
 *
 * @param sqlLocations Flyway locations such as `classpath:db/migrations`.
 */
internal class MigrationClassLoader(
    private val plugin: ClassLoader,
    sqlLocations: List<String>,
) : ClassLoader(DatabaseApi::class.java.classLoader) {

    private val sqlRoots = sqlLocations.map { it.removePrefix(CLASSPATH_PREFIX).trim('/') }

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
        return sqlRoots.any { root -> root.isEmpty() || path == root || path.startsWith("$root/") }
    }

    private companion object {
        const val CLASSPATH_PREFIX = "classpath:"
    }
}
