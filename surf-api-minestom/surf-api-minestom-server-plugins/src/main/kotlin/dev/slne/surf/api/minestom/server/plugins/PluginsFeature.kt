package dev.slne.surf.api.minestom.server.plugins

import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.server.SurfMinestomFeature
import dev.slne.surf.api.minestom.server.SurfMinestomServer
import dev.slne.surf.api.minestom.server.SurfMinestomServerBuilder
import dev.slne.surf.api.minestom.server.SurfMinestomServerDsl
import dev.slne.surf.api.minestom.server.plugins.impl.PluginDiscovery
import dev.slne.surf.api.minestom.server.plugins.impl.PluginManagerImpl
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

/**
 * Configures [withPlugins].
 */
@SurfMinestomServerDsl
class PluginsFeatureBuilder internal constructor() {

    /** The directory plugin jars are loaded from, and their data directories are created in. */
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

        val candidates = buildList {
            if (settings.loadClassPathPlugins) addAll(PluginDiscovery.fromClassPath(serverClassLoader))
            addAll(PluginDiscovery.fromDirectory(directory))
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

    companion object {
        const val ID = "plugins"
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
