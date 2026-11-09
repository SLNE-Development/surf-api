package dev.slne.surf.api.minestom.server.impl.di

import dev.slne.surf.api.core.server.component.environment.EnvironmentConfig
import dev.slne.surf.api.core.server.component.property.PropertyService
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.minestom.plugin.PluginManager
import dev.slne.surf.api.minestom.plugin.SurfMinestomPlugin
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Answers the container's platform questions on a Minestom server.
 *
 * A Minestom process has exactly two kinds of container owner: a plugin the server loaded, which
 * gets a child container, and the server itself, which owns the root. Every member below
 * distinguishes those two, and anything that is not a [SurfMinestomPlugin] is the server — a
 * server without the plugins feature never produces any other owner, so the server case is the
 * fallback rather than a failure.
 *
 * @param serverClassLoader the loader surf-api itself was loaded by, which is the server's own
 * class loader at runtime and a parameter so that a test can supply another one
 */
@OptIn(InternalSurfApi::class)
class MinestomContainerPlatform(
    private val serverClassLoader: ClassLoader =
        MinestomContainerPlatform::class.java.classLoader,
) : ContainerPlatform {

    /**
     * The loader whose bean metadata makes up [owner]'s container.
     *
     * A plugin loaded from a jar carries its own `PluginClassLoader`, which is the loader of its
     * main class and therefore of its beans. A plugin shaded into the server has no loader of its
     * own and its classes come from the server's, which is also what the plugin manager falls back
     * to when it creates such a plugin.
     *
     * Unlike Paper, Minestom needs no guard against a plugin that bundles surf-api itself:
     * `PluginClassLoader` delegates parent-first, so the server's copy of an interface wins over a
     * bundled one and every type on the server class path is loaded exactly once. Reading the
     * loader off the owner's own class is therefore the whole answer, and it stays correct for a
     * plugin whose main class happens to live on the server class path as well.
     */
    override fun classLoader(owner: Any): ClassLoader = when (owner) {
        is SurfMinestomPlugin -> owner.javaClass.classLoader ?: serverClassLoader
        else -> serverClassLoader
    }

    /**
     * The owner's own metadata, read from its own classpath entry rather than from everything its
     * loader can see.
     *
     * This is the platform the warning on [ContainerPlatform.beanSources] is about. A jar plugin's
     * `PluginClassLoader` overrides `loadClass` to look at the server first, but it does not
     * override `getResources`, which is inherited from `URLClassLoader` and is both parent-first
     * and additive. Since surf-api itself sits on the server's classpath here, `getResources` from
     * a plugin's loader returns surf-api's bean metadata as well as the plugin's, and the plugin's
     * container would declare every surf-api bean a second time. `findResources` searches a
     * `URLClassLoader`'s own entries only, which is exactly the question being asked.
     *
     * A plugin shaded into the server has no loader of its own, so there is no entry to separate it
     * by and this falls back to the whole server classpath. That is the honest answer rather than a
     * limitation: such a plugin shares the server's classes, its libraries and its lifetime, so its
     * beans belong to the root container and it must not be given a child of its own — a child
     * would declare the server's beans again and build a second instance of each singleton.
     */
    override fun beanSources(owner: Any): List<URL> = when (val loader = classLoader(owner)) {
        is URLClassLoader -> loader.findResources(BeanMetadata.DIRECTORY).toList()
        else -> loader.getResources(BeanMetadata.DIRECTORY).toList()
    }

    /**
     * Whether the server loaded the *Minestom* plugin [pluginId].
     *
     * `@ConditionalOnPlugin` reads the same on every platform, but the id spaces do not overlap: a
     * Minestom plugin is described by a `minestom-plugin.json` and has nothing to do with a Paper
     * plugin of the same name. A bean that is guarded on a plugin id therefore has to be written
     * for the platform it runs on.
     *
     * Loading plugins is opt-in (`withPlugins`), so a server without the feature has no plugins at
     * all and the answer is always `false`.
     */
    override fun isPluginLoaded(pluginId: String): Boolean =
        pluginManagerOrNull()?.isLoaded(pluginId) == true

    /**
     * Where the container reports about [owner].
     *
     * A plugin already has the logger the server gave it, named after the plugin, so its skipped
     * beans show up next to the rest of its output; the server's own container logs under this
     * class.
     */
    override fun logger(owner: Any): PlatformLogger = ComponentPlatformLogger(
        when (owner) {
            is SurfMinestomPlugin -> owner.logger
            else -> LOGGER
        }
    )

    /**
     * The directory [owner]'s configuration files live in.
     *
     * A plugin has `plugins/<id>`, created before the plugin is constructed. The server's own
     * configuration lives in its working directory, which is where it reads everything else from.
     */
    override fun dataPath(owner: Any): Path = when (owner) {
        is SurfMinestomPlugin -> owner.dataDirectory
        else -> SERVER_DIRECTORY
    }

    /**
     * The environments the process runs in.
     *
     * Read through [EnvironmentConfig], which resolves `SURF_ENVIRONMENT` through the accessor that
     * also looks in a `.env` file and treats a blank value as absent. Reading the variable straight
     * from the process environment instead would make a `@ConditionalOnEnvironment` silently fail
     * to match whenever the environment is configured in that file, which is the usual case in
     * development.
     */
    override fun environments(): Set<String> = EnvironmentConfig.getEnvironments().toSet()

    /**
     * Returns the configuration value at [key] in [file], or `null` when it is not set.
     *
     * Delegates to [PropertyService], which reads an arbitrary path out of an arbitrary YAML file
     * under the data directory, caches the parsed file and resolves an empty [file] to
     * [ConditionalOnProperty.DEFAULT_PROPERTIES_FILE]. An absent file and an absent key both come
     * back as `null`, because for a condition absence is an answer and not a failure.
     *
     * A failure to read a file that is there is not caught. It would otherwise be
     * indistinguishable from the property being unset, and `matchIfMissing` would then decide — so
     * an unreadable configuration file could bring a server up with every guarded feature on the
     * wrong side of its default, with nothing to say so. A start that stops is the cheaper outcome.
     */
    override fun property(owner: Any, key: List<String>, file: String): String? =
        PropertyService.getProperty(dataPath(owner), key.toTypedArray(), file)

    /**
     * The server's plugin manager, or `null` when the server does not load plugins.
     *
     * [PluginManager.instance] is the only published way to reach it and throws for a server
     * without the plugins feature, which is a legitimate configuration rather than an error here.
     */
    private fun pluginManagerOrNull(): PluginManager? = try {
        PluginManager.instance
    } catch (_: IllegalStateException) {
        null
    }

    private companion object {
        val LOGGER = ComponentLogger.logger()

        /**
         * The server's working directory, resolved once because the process never changes it.
         */
        val SERVER_DIRECTORY: Path = Path("").toAbsolutePath()
    }
}

/**
 * Reports the container's messages through a [ComponentLogger].
 *
 * The container declares its own narrow logger so that the module holding its contracts needs no
 * logging framework; on this platform every owner already has a logger, so there is nothing to do
 * but forward.
 */
@OptIn(InternalSurfApi::class)
private class ComponentPlatformLogger(private val delegate: ComponentLogger) : PlatformLogger {

    override fun info(message: String) = delegate.info(message)

    override fun warn(message: String) = delegate.warn(message)

    override fun error(message: String, throwable: Throwable?) =
        if (throwable == null) delegate.error(message) else delegate.error(message, throwable)

    override fun debug(message: String) = delegate.debug(message)
}
