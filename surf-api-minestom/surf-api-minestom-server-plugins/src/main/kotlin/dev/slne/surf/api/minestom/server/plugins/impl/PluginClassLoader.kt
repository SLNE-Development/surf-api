package dev.slne.surf.api.minestom.server.plugins.impl

import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Loads the classes of one plugin jar.
 *
 * Classes are looked up in the server first, then in the plugin's own jar and finally in the jars
 * of the plugins it depends on, so a plugin sees the server, itself and its dependencies, but no
 * other plugin.
 */
internal class PluginClassLoader(
    val pluginId: String,
    jar: Path,
    parent: ClassLoader,
) : URLClassLoader("plugin-$pluginId", arrayOf(jar.toUri().toURL()), parent) {

    /** The class loaders of every plugin this one depends on, directly or not. */
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

    /** A class from this plugin's own jar, without asking the server or other plugins. */
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
