package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.plugins.impl.PluginDiscovery
import dev.slne.surf.api.minestom.server.plugins.impl.PluginManagerImpl
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/**
 * Configures [withPlugins].
 */
@SurfMinestomServerDsl
class PluginsFeatureBuilder internal constructor() {

    /**
     * The directory plugin jars are loaded from, and their data directories are created in.
     *
     * When the server declares plugin dependencies, start it with `-Dsurf.minestom.plugins=<dir>`
     * after changing this, so that the bootstrap finds them in the same directory.
     */
    var directory: Path = Path("plugins")

    /**
     * Whether plugins shaded into the server itself are loaded too, e.g. the plugin modules of the
     * same Gradle project.
     */
    var loadClassPathPlugins: Boolean = true
}

/**
 * Loads the plugins in the `plugins` directory, and those shaded into the server.
 *
 * Plugins extend [dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin] and describe themselves in
 * a `minestom-plugin.json`, which the `dev.slne.surf.api.gradle.minestom` Gradle plugin generates.
 * Each plugin jar gets its own class loader that sees the server, the plugin itself and the
 * plugins it depends on.
 *
 * Plugins are loaded while the server starts, enabled on its first tick and disabled, in reverse
 * order, when it shuts down.
 *
 * The server's own code can only use plugins on its class path. Plugins in the directory that the
 * server declares in `surfMinestomServerApi { pluginDependencies { } }` are put there by the
 * bootstrap of the server jar, together with the plugins they depend on, and loaded from there.
 */
@OptIn(InternalSurfApi::class)
class PluginsFeature internal constructor(
    private val settings: PluginsFeatureBuilder,
) : SurfMinestomFeature {

    override val id: String = ID

    /** Plugins rely on the other features, so they are loaded after all of them. */
    override val loadPriority: Int = Int.MAX_VALUE

    private var manager: PluginManagerImpl? = null

    override fun load(api: SurfMinestomServer) {
        val directory = settings.directory.createDirectories()
        val serverClassLoader = PluginsFeature::class.java.classLoader

        val classPath = if (settings.loadClassPathPlugins) PluginDiscovery.fromClassPath(serverClassLoader) else emptyList()
        val classPathIds = classPath.mapTo(mutableSetOf()) { it.meta.id }
        val serverPluginJars = serverPluginJars()

        val candidates = buildList {
            addAll(classPath)
            for (candidate in PluginDiscovery.fromDirectory(directory)) {
                val jar = candidate.jar?.toAbsolutePath()?.normalize()
                when {
                    jar !in serverPluginJars -> add(candidate)
                    // Already found through the class path index the jar carries
                    candidate.meta.id in classPathIds -> Unit
                    else -> add(candidate.copy(jar = null))
                }
            }
        }

        val manager = PluginManagerImpl(directory, api.eventNode, serverClassLoader)
        this.manager = manager
        PluginManager.install(manager)

        manager.load(candidates)
    }

    override fun enable(api: SurfMinestomServer) {
        manager?.enable()
    }

    override fun disable(api: SurfMinestomServer) {
        manager?.disableAll()
        manager = null
        PluginManager.install(null)
    }

    /** The plugin jars the bootstrap put on the server's class path. */
    private fun serverPluginJars(): Set<Path> =
        System.getProperty(SERVER_PLUGINS_PROPERTY).orEmpty()
            .split(File.pathSeparator)
            .filter(String::isNotEmpty)
            .mapTo(mutableSetOf()) { Path(it).toAbsolutePath().normalize() }

    companion object {
        const val ID = "plugins"

        /** Set by the bootstrap of the server jar, see `ServerPlugins` there. */
        private const val SERVER_PLUGINS_PROPERTY = "surf.minestom.server-plugins"
    }
}

/**
 * Loads the plugins in the `plugins` directory, and those shaded into the server.
 *
 * Plugins are loaded after every other feature, wherever this is called.
 *
 * @see PluginsFeature
 */
fun SurfMinestomServerBuilder.withPlugins(block: PluginsFeatureBuilder.() -> Unit = {}) {
    install(PluginsFeature(PluginsFeatureBuilder().apply(block)))
}
