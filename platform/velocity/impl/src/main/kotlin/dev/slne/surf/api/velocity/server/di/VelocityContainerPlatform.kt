package dev.slne.surf.api.velocity.server.di

import com.velocitypowered.api.proxy.ProxyServer
import dev.slne.surf.api.core.server.component.environment.EnvironmentConfig
import dev.slne.surf.api.core.server.component.property.PropertyService
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Path

/**
 * Answers the container's platform questions on a Velocity proxy.
 *
 * Every owner here is a plugin, surf-api included, and Velocity gives each one its own
 * `PluginClassLoader` and its own data directory under `plugins/`. What it does not give is a way
 * to reach another plugin's injected logger or data directory, so both are derived from surf-api's
 * own: the directory from its sibling path, the logger from the plugin's id. Deriving beats
 * guessing here because the derivation comes from a path Velocity itself injected.
 *
 * @param server the proxy, for the plugin list
 * @param dataDirectory surf-api's own data directory, which is `plugins/surf-api-velocity`; its
 * parent is where every other plugin's directory lives
 */
@InternalSurfApi
internal class VelocityContainerPlatform(
    private val server: ProxyServer,
    private val dataDirectory: Path,
) : ContainerPlatform {

    /**
     * The owning plugin's own loader.
     *
     * Read off the owner's class rather than from the plugin manager, which exposes no loader. For
     * a plugin's main class the two are the same object; for anything else the owner is not a
     * plugin instance and the loader of whichever jar declared it is still the right answer,
     * because that jar is what carries its beans.
     */
    override fun classLoader(owner: Any): ClassLoader =
        owner.javaClass.classLoader ?: javaClass.classLoader

    /**
     * The owner's own classpath entry, not everything its loader can see.
     *
     * Velocity's `PluginClassLoader` extends `URLClassLoader` and overrides class loading so a
     * plugin can reach its siblings, but it does not override `getResources`, which stays
     * parent-first and additive. On this platform surf-api is a sibling rather than a parent, so
     * the warning on [ContainerPlatform.beanSources] does not bite in the way it does on Minestom
     * — but the system classpath still would, and `findResources` is in any case the exact
     * question being asked: which beans does *this* plugin declare.
     */
    override fun beanSources(owner: Any): List<URL> = when (val loader = classLoader(owner)) {
        is URLClassLoader -> loader.findResources(BeanMetadata.DIRECTORY).toList()
        else -> loader.getResources(BeanMetadata.DIRECTORY).toList()
    }

    /**
     * Whether the proxy loaded the plugin [pluginId].
     *
     * Velocity's ids are its own: a `@ConditionalOnPlugin` written for a Paper plugin says nothing
     * here, so a bean guarded on an id belongs to the platform it names.
     */
    override fun isPluginLoaded(pluginId: String): Boolean = server.pluginManager.isLoaded(pluginId)

    /**
     * A logger named after the owning plugin, or after this class for a non-plugin owner.
     *
     * Velocity injects a logger into each plugin and keeps no registry of them, so the container
     * cannot borrow the owner's. Naming one after the plugin's id gets the same thing that matters:
     * a skipped bean is reported against the plugin it belongs to.
     */
    override fun logger(owner: Any): PlatformLogger =
        ComponentPlatformLogger(ComponentLogger.logger(idOf(owner) ?: javaClass.name))

    /**
     * The owning plugin's data directory.
     *
     * Velocity puts every plugin's directory next to the others under `plugins/`, and surf-api's
     * own was injected, so a sibling's path is its parent plus the plugin's id. An owner that is
     * not a plugin gets surf-api's own directory, which is where a bean of surf-api's belongs.
     */
    override fun dataPath(owner: Any): Path =
        idOf(owner)?.let { id -> dataDirectory.parent.resolve(id) } ?: dataDirectory

    /**
     * The environments the process runs in.
     *
     * Through [EnvironmentConfig], which resolves `SURF_ENVIRONMENT` with the accessor that also
     * reads a `.env` file and treats a blank value as absent. Reading the variable straight from
     * the process environment would make a `@ConditionalOnEnvironment` miss an environment
     * configured in that file, which is the usual case in development.
     */
    override fun environments(): Set<String> = EnvironmentConfig.getEnvironments().toSet()

    /**
     * Returns the configuration value at [key] in [file], or `null` when it is not set.
     *
     * [PropertyService] reads it under [dataPath], caches the parsed file and resolves an empty
     * [file] to [ConditionalOnProperty.DEFAULT_PROPERTIES_FILE]. An absent file and an absent key
     * both come back as `null`, because for a condition absence is an answer and not a failure.
     *
     * A failure to read a file that is there is not caught. It would otherwise be
     * indistinguishable from the property being unset, and `matchIfMissing` would then decide — so
     * an unreadable configuration file could bring the proxy up with every guarded feature on the
     * wrong side of its default, with nothing to say so.
     */
    override fun property(owner: Any, key: List<String>, file: String): String? =
        PropertyService.getProperty(dataPath(owner), key.toTypedArray(), file)

    /** The plugin id [owner] is the instance of, or `null` when it is not a plugin. */
    private fun idOf(owner: Any): String? =
        server.pluginManager.fromInstance(owner).orElse(null)?.description?.id
}

/**
 * Reports the container's messages through a [ComponentLogger].
 *
 * The container declares its own narrow logger so the module holding its contracts needs no
 * logging framework; here there is nothing to do but forward.
 */
@InternalSurfApi
private class ComponentPlatformLogger(private val delegate: ComponentLogger) : PlatformLogger {

    override fun info(message: String) = delegate.info(message)

    override fun warn(message: String) = delegate.warn(message)

    override fun error(message: String, throwable: Throwable?) =
        if (throwable == null) delegate.error(message) else delegate.error(message, throwable)

    override fun debug(message: String) = delegate.debug(message)
}
