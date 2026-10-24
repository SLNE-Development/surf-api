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
     * When the server declares plugin dependencies and this value is changed, also start it with
     * `-Dsurf.minestom.plugins=<dir>`, so that the bootstrap reads the same directory.
     */
    var directory: Path = Path("plugins")

    /**
     * Whether plugins shaded into the server itself are loaded too, e.g. the plugin modules of the
     * same Gradle project.
     */
    var loadClassPathPlugins: Boolean = true

    /**
     * Whether the server fails to start when a plugin cannot be loaded or enabled.
     *
     * A plugin cannot be loaded when its description cannot be read, it is rejected, fails to be
     * created, throws from `onLoad` or requires a plugin that could not be loaded; it cannot be
     * enabled when it throws from `onEnable` or requires a plugin that could not be enabled. A jar
     * in [directory] without a `minestom-plugin.json` is no plugin and does not count. When
     * `false`, the server logs the failure and runs without that plugin and the plugins that depend
     * on it.
     */
    var requireAll: Boolean = false
}

/**
 * Loads the plugins in the `plugins` directory and those shaded into the server.
 *
 * Plugins extend [dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin] and are described by a
 * `minestom-plugin.json`, which the `dev.slne.surf.api.gradle.minestom` Gradle plugin generates.
 * Each plugin jar loaded from the `plugins` directory gets its own class loader, which sees the
 * server, the plugin itself and the plugins it depends on, but no other plugin.
 *
 * Plugins load in dependency order while the server starts, are enabled right after they are loaded,
 * before [dev.slne.surf.api.minestom.server.surfMinestomServer] returns and so before the server
 * accepts players, and are disabled in reverse order on shutdown.
 *
 * Each plugin's data directory, `<directory>/<id>`, is created before the plugin is constructed.
 * A plugin that fails to load or enable is skipped, unless [PluginsFeatureBuilder.requireAll] is
 * set.
 *
 * The server's own code cannot see into a plugin class loader, so it can only use plugins on the
 * system class path. The bootstrap of the server jar places the plugins declared in
 * `surfMinestomServerApi { pluginDependencies { } }`, together with their dependencies, there, and
 * the feature loads them from there.
 */
@OptIn(InternalSurfApi::class)
class PluginsFeature internal constructor(
    private val settings: PluginsFeatureBuilder,
) : SurfMinestomFeature {

    override val id: String = ID

    /**
     * Loads after every other feature, so that plugins can use them.
     */
    override val loadPriority: Int = Int.MAX_VALUE

    private var manager: PluginManagerImpl? = null

    override fun load(api: SurfMinestomServer) {
        val directory = settings.directory.createDirectories()
        val serverClassLoader = PluginsFeature::class.java.classLoader

        val discoveryFailures = linkedMapOf<String, String>()
        val classPath = if (settings.loadClassPathPlugins) {
            PluginDiscovery.fromClassPath(serverClassLoader, discoveryFailures)
        } else {
            emptyList()
        }
        val classPathIds = classPath.mapTo(mutableSetOf()) { it.meta.id }
        val serverPluginJars = serverPluginJars()

        val candidates = buildList {
            addAll(classPath)
            for (candidate in PluginDiscovery.fromDirectory(directory, discoveryFailures)) {
                val jar = candidate.jar?.toAbsolutePath()?.normalize()
                when {
                    jar !in serverPluginJars -> add(candidate)
                    // The class path index already lists the plugin of this bootstrap-placed jar
                    candidate.meta.id in classPathIds -> Unit
                    else -> add(candidate.copy(jar = null))
                }
            }
        }

        val manager = PluginManagerImpl(directory, api.eventNode, serverClassLoader)
        this.manager = manager
        PluginManager.install(manager)

        val failures = discoveryFailures + manager.load(candidates)
        if (failures.isNotEmpty() && settings.requireAll) {
            // Rolls back here, so that no plugin stays loaded however the runtime handles a failing load
            disable(api)
            throw requireAllFailure("loaded", failures.keys)
        }
    }

    override fun enable(api: SurfMinestomServer) {
        val failures = manager?.enable().orEmpty()
        if (failures.isNotEmpty() && settings.requireAll) {
            throw requireAllFailure("enabled", failures.keys)
        }
    }

    override fun disable(api: SurfMinestomServer) {
        manager?.disableAll()
        manager = null
        PluginManager.install(null)
    }

    private fun requireAllFailure(action: String, ids: Collection<String>) = IllegalStateException(
        "Plugins could not be $action: ${ids.joinToString()}. They are required by requireAll, " +
                "the reasons are logged above"
    )

    private fun serverPluginJars(): Set<Path> =
        System.getProperty(SERVER_PLUGINS_PROPERTY).orEmpty()
            .split(File.pathSeparator)
            .filter(String::isNotEmpty)
            .mapTo(mutableSetOf()) { Path(it).toAbsolutePath().normalize() }

    companion object {
        const val ID = "plugins"

        /**
         * System property holding the plugin jars the bootstrap placed on the system class path,
         * separated by [File.pathSeparator]. Set by `ServerPlugins` in the bootstrap module.
         */
        private const val SERVER_PLUGINS_PROPERTY = "surf.minestom.server-plugins"
    }
}

/**
 * Installs plugin loading.
 *
 * Plugins load after every other feature, regardless of where this is called.
 *
 * @see PluginsFeature
 */
fun SurfMinestomServerBuilder.withPlugins(block: PluginsFeatureBuilder.() -> Unit = {}) {
    install(PluginsFeature(PluginsFeatureBuilder().apply(block)))
}
