package dev.slne.surf.api.minestom.server.plugins.impl

import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Loads the classes of one plugin jar.
 *
 * Lookup order is the server class loader, the plugin's own jar, then the jars of [dependencies].
 * A plugin therefore sees the server, itself and its dependencies, but no other plugin.
 */
internal class PluginClassLoader(
    val pluginId: String,
    jar: Path,
    parent: ClassLoader,
) : URLClassLoader("plugin-$pluginId", arrayOf(jar.toUri().toURL()), parent) {

    /**
     * The class loaders of all plugins this one depends on, directly or transitively. Plugins
     * on the system class path have no loader and are not listed.
     */
    @Volatile
    var dependencies: List<PluginClassLoader> = emptyList()

    override fun loadClass(name: String, resolve: Boolean): Class<*> {
        synchronized(getClassLoadingLock(name)) {
            val loaded = findLoadedClass(name)
                ?: fromParent(name)
                ?: fromOwnJar(name)
                ?: dependencies.firstNotNullOfOrNull { dependency -> dependency.findOwnClass(name) }
                ?: throw ClassNotFoundException(name)

            if (resolve) resolveClass(loaded)
            return loaded
        }
    }

    /**
     * Returns the class from this plugin's own jar, or `null`. Does not consult the server or other
     * plugins.
     */
    fun findOwnClass(name: String): Class<*>? = synchronized(getClassLoadingLock(name)) {
        findLoadedClass(name) ?: fromOwnJar(name)
    }

    private fun fromParent(name: String): Class<*>? = try {
        parent.loadClass(name)
    } catch (_: ClassNotFoundException) {
        null
    }

    private fun fromOwnJar(name: String): Class<*>? = try {
        findClass(name)
    } catch (_: ClassNotFoundException) {
        null
    }

    override fun toString(): String = "PluginClassLoader($pluginId)"

    private companion object {
        init {
            registerAsParallelCapable()
        }
    }
}
