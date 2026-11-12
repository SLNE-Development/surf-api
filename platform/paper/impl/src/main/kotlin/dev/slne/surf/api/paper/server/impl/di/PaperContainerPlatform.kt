package dev.slne.surf.api.paper.server.impl.di

import dev.slne.surf.api.core.server.component.environment.EnvironmentConfig
import dev.slne.surf.api.core.server.component.property.PropertyService
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.paper.extensions.pluginManager
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import org.bukkit.plugin.java.JavaPlugin
import java.net.URL
import java.nio.file.Path

/**
 * Answers the container's platform questions on Paper.
 *
 * Every owner on this platform is a plugin, which is what makes the answers short: Paper already
 * gives each plugin its own classloader, its own logger and its own data directory, so each member
 * is a matter of asking the right plugin rather than of inventing a per-application notion of any
 * of them. An owner that is not a plugin is refused rather than given the server's own answers,
 * because every such owner would otherwise share one container's worth of beans.
 */
@InternalSurfApi
class PaperContainerPlatform : ContainerPlatform {

    /**
     * The owning plugin's own `PaperPluginClassLoader`.
     *
     * Three properties of that loader are what the container relies on. Its
     * `getResources("META-INF/surf-api/beans")` sees that plugin's jar and the libraries loaded for
     * it, and nothing else, so per-plugin bean discovery needs no filtering and cannot pick up a
     * neighbour's beans. Its `Class.forName` reaches the server, the plugin, the plugin's libraries
     * and the plugins it declares, in that order, which is exactly the set a plugin's beans may be
     * written against. And because surf-api's own `paper-plugin.yml` sets
     * `has-open-classloader: false`, surf-api's loader can never reach a downstream plugin's
     * classes, so the root container — which is handed surf-api's own loader — can only ever hold
     * beans declared by surf-api itself.
     *
     * The loader is read by reflection because `JavaPlugin.getClassLoader` is `protected`, and by
     * *plain* reflection rather than through surf-api's own proxy service, which is the point worth
     * remembering: this class answers the container's questions, so it is asked them while the
     * container is still being built. Anything it reaches for that is itself a bean would be a
     * request the container cannot serve yet — surf-api's proxy service is a bean, so using it here
     * stopped the server from starting at all.
     *
     * `owner.javaClass.classLoader` is not a substitute either: it happens to return the same
     * object for a plugin's main class, but for any other owner it returns the loader of whichever
     * jar declared that class, which is a different plugin's beans or surf-api's.
     *
     * @throws IllegalStateException if [owner] is not a plugin
     */
    override fun classLoader(owner: Any): ClassLoader =
        GET_CLASS_LOADER.invoke(pluginOf(owner)) as ClassLoader

    /**
     * The plugin's own metadata, which on Paper is what its loader already answers with.
     *
     * `PaperPluginClassLoader` overrides `getResources` to return its own jar and the libraries
     * loaded for it, and nothing from its parent or from the plugins it depends on. That makes the
     * general warning on [ContainerPlatform.beanSources] inapplicable here: this platform's loader
     * is own-only for resources even though it delegates upwards for classes, so the container sees
     * exactly the beans that plugin declares.
     *
     * @throws IllegalStateException if [owner] is not a plugin
     */
    override fun beanSources(owner: Any): List<URL> =
        classLoader(owner).getResources(BeanMetadata.DIRECTORY).toList()

    /**
     * Whether a plugin with [pluginId] is loaded.
     *
     * Loaded, not enabled: the plugin manager knows a plugin from the moment its classes are on the
     * classpath, which is before its `onEnable` has run. `@ConditionalOnPlugin` therefore holds for
     * a plugin that is present but has not started yet, which is the useful reading during a load
     * phase — a bean guarded this way is one whose classes need the other plugin's, and those are
     * there as soon as it is loaded.
     */
    override fun isPluginLoaded(pluginId: String): Boolean =
        pluginManager.getPlugin(pluginId) != null

    /**
     * The owning plugin's own logger.
     *
     * A plugin's `ComponentLogger` already carries its name, so a skipped bean is reported against
     * the plugin it belongs to without the container having to say which one, and the server's own
     * log configuration applies to it.
     *
     * @throws IllegalStateException if [owner] is not a plugin
     */
    override fun logger(owner: Any): PlatformLogger =
        ComponentPlatformLogger(pluginOf(owner).componentLogger)

    /**
     * The owning plugin's data folder.
     *
     * @throws IllegalStateException if [owner] is not a plugin
     */
    override fun dataPath(owner: Any): Path = pluginOf(owner).dataPath

    /**
     * The environments named by `SURF_ENVIRONMENT`, or production when it is not set.
     *
     * Read through [EnvironmentConfig], which goes to the accessor that consults a `.env` file as
     * well as the process environment and treats a blank value as absent. The repository also has a
     * plain `System.getenv` path, and using it here would make a variable set only in `.env`
     * invisible: every `@ConditionalOnEnvironment` would then quietly fall back to production and
     * skip the beans that were meant to run.
     */
    override fun environments(): Set<String> = EnvironmentConfig.getEnvironments().toSet()

    /**
     * The configuration value at [key] in [file], or `null` when it is not set.
     *
     * [PropertyService] answers this: it reads the YAML file under the plugin's data folder, caches
     * the parsed tree so a container full of guarded beans opens the file once, and resolves a
     * blank [file] to [ConditionalOnProperty.DEFAULT_PROPERTIES_FILE]. An absent file and an absent
     * key both come back as `null`, which is the answer `@ConditionalOnProperty` needs rather than
     * a failure.
     *
     * A failure to read a file that is there is not caught. It would otherwise be indistinguishable
     * from the property being unset, and `matchIfMissing` would then decide — so an unreadable
     * configuration file could bring a plugin up with every guarded feature on the wrong side of
     * its default, and nothing would say so. A load that stops is the cheaper outcome.
     */
    override fun property(owner: Any, key: List<String>, file: String): String? =
        PropertyService.getProperty(dataPath(owner), key.toTypedArray(), file)

    /**
     * Returns [owner] as a plugin, or fails saying what was passed instead.
     *
     * A Paper container's owner has to be a plugin because every answer here comes from one. The
     * message names the class as well as the value, since the owner is whatever the caller handed
     * to the container host and the mistake is usually a plugin's main class in place of its
     * instance.
     */
    private fun pluginOf(owner: Any): JavaPlugin = owner as? JavaPlugin ?: error(
        "A Paper container's owner has to be a plugin, but $owner (${owner.javaClass.name}) is " +
                "not a JavaPlugin. The classloader, logger and data folder a container needs all " +
                "belong to a plugin, so pass the plugin instance itself."
    )

    private companion object {

        /** `JavaPlugin.getClassLoader`, which is `protected`, opened once. */
        val GET_CLASS_LOADER = JavaPlugin::class.java
            .getDeclaredMethod("getClassLoader")
            .apply { isAccessible = true }
    }
}

/**
 * Reports the container's messages through a plugin's [ComponentLogger].
 *
 * The container asks for four levels and slf4j has all four, so this adapter exists only to keep
 * the contract module free of a logging dependency.
 */
@InternalSurfApi
private class ComponentPlatformLogger(private val delegate: ComponentLogger) : PlatformLogger {

    override fun info(message: String) = delegate.info(message)

    override fun warn(message: String) = delegate.warn(message)

    override fun error(message: String, throwable: Throwable?) {
        if (throwable == null) delegate.error(message) else delegate.error(message, throwable)
    }

    override fun debug(message: String) = delegate.debug(message)
}
