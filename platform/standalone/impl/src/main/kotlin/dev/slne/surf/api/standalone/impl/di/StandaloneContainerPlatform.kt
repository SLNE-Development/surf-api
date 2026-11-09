package dev.slne.surf.api.standalone.impl.di

import dev.slne.surf.api.core.server.component.environment.EnvironmentConfig
import dev.slne.surf.api.core.server.component.property.PropertyService
import dev.slne.surf.api.di.ContainerPlatform
import dev.slne.surf.api.di.PlatformLogger
import dev.slne.surf.api.di.condition.ConditionalOnProperty
import dev.slne.surf.api.di.definition.BeanMetadata
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import java.net.URL
import java.nio.file.Path
import kotlin.io.path.Path

/**
 * Answers the container's platform questions outside a Minecraft server.
 *
 * A standalone process is the simple case, and the reason is worth stating: there is one
 * classloader and one application. surf-api and whatever embeds it are on the same classpath, so
 * there is no boundary to isolate and nothing to tell apart — every question below has one answer
 * regardless of who asks. A standalone process therefore has a root container and no children; a
 * child would read the same classpath and build a second instance of every bean.
 *
 * @param classLoader the loader the process was started with, a parameter so that a test can hand
 * in another one
 */
@InternalSurfApi
internal class StandaloneContainerPlatform(
    private val classLoader: ClassLoader = StandaloneContainerPlatform::class.java.classLoader,
) : ContainerPlatform {

    /**
     * The process's own classloader, whoever asks.
     *
     * Reading it off the owner would be wrong rather than merely unnecessary: an owner declared in
     * a library would hand back that library's loader, and on a flat classpath that is a narrower
     * view of the same classes for no reason.
     */
    override fun classLoader(owner: Any): ClassLoader = classLoader

    /**
     * Everything on the classpath, which here is the honest answer rather than a loose one.
     *
     * The warning on [ContainerPlatform.beanSources] is about a platform where surf-api sits above
     * an application and `getResources` would hand the application surf-api's beans as well as its
     * own. There is no "as well as" here: the embedding application's beans and surf-api's are on
     * one classpath and belong in one container, which is exactly what this returns.
     */
    override fun beanSources(owner: Any): List<URL> =
        classLoader.getResources(BeanMetadata.DIRECTORY).toList()

    /**
     * Always `false`: a standalone process has no plugins.
     *
     * `@ConditionalOnPlugin` is about a server's plugin list, and there is none, so a bean guarded
     * that way never matches here. That is the answer rather than an omission — a standalone build
     * that wants a feature switched by its surroundings uses `@ConditionalOnProperty` or
     * `@ConditionalOnEnvironment`.
     */
    override fun isPluginLoaded(pluginId: String): Boolean = false

    /** Where the container reports; there is one logger for the process. */
    override fun logger(owner: Any): PlatformLogger = StandalonePlatformLogger

    /**
     * The working directory.
     *
     * A standalone process reads the rest of its configuration from where it was started, so the
     * container's idea of "the data directory" has to agree with that or a `@ConditionalOnProperty`
     * would look in a different file than the code it guards.
     */
    override fun dataPath(owner: Any): Path = WORKING_DIRECTORY

    /**
     * The environments the process runs in.
     *
     * Through [EnvironmentConfig], which resolves `SURF_ENVIRONMENT` with the accessor that also
     * reads a `.env` file and treats a blank value as absent. Going to the process environment
     * directly would make a `@ConditionalOnEnvironment` miss an environment configured in that
     * file, which is the usual case outside production.
     */
    override fun environments(): Set<String> = EnvironmentConfig.getEnvironments().toSet()

    /**
     * Returns the configuration value at [key] in [file], or `null` when it is not set.
     *
     * [PropertyService] reads it, caches the parsed file and resolves an empty [file] to
     * [ConditionalOnProperty.DEFAULT_PROPERTIES_FILE]. An absent file and an absent key both come
     * back as `null`, because for a condition absence is an answer.
     *
     * A failure to read a file that is there is not caught. It would otherwise be
     * indistinguishable from the property being unset, and `matchIfMissing` would then decide — so
     * an unreadable file could start the process with every guarded feature on the wrong side of
     * its default and nothing to say so.
     */
    override fun property(owner: Any, key: List<String>, file: String): String? =
        PropertyService.getProperty(dataPath(owner), key.toTypedArray(), file)

    private companion object {

        /** Resolved once, because a process never changes its working directory. */
        val WORKING_DIRECTORY: Path = Path("").toAbsolutePath()
    }
}

/**
 * Reports the container's messages through the process's logger.
 *
 * An object rather than a class: there is one logger for one process, and the container asks for a
 * logger per owner only because a server has one per plugin.
 */
@InternalSurfApi
private object StandalonePlatformLogger : PlatformLogger {

    private val logger = ComponentLogger.logger("surf-api")

    override fun info(message: String) = logger.info(message)

    override fun warn(message: String) = logger.warn(message)

    override fun error(message: String, throwable: Throwable?) =
        if (throwable == null) logger.error(message) else logger.error(message, throwable)

    override fun debug(message: String) = logger.debug(message)
}
