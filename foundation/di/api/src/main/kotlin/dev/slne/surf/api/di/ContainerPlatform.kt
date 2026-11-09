package dev.slne.surf.api.di

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.net.URL
import java.nio.file.Path

/**
 * Everything the container cannot work out for itself and has to ask the platform.
 *
 * The container is plain JVM logic: it resolves types, orders a graph and drives a lifecycle. Where
 * the bean classes are loaded from, whether a plugin is present, where to log, where configuration
 * lives and what it says are all platform questions, and they are the only ones. A platform
 * contributes an implementation of this interface and the rest of the container runs unchanged —
 * which is also what lets the tests drive it without a server.
 *
 * Passed to the container at construction rather than inherited from, so a test can supply a
 * different one per container.
 */
@InternalSurfApi
interface ContainerPlatform {

    /**
     * The classloader that loads this application's bean classes.
     *
     * On a platform that isolates applications from each other this is that application's own
     * loader, which is exactly why two applications get two containers. It is asked to *load* a
     * class, so it may and should delegate upwards: a bean's constructor parameter is usually a
     * type from surf-api or the platform.
     *
     * Which beans an application has is a separate question, answered by [beanSources]. They are
     * separate because the obvious implementation of the second one in terms of the first is wrong
     * on at least one platform — see that member.
     */
    fun classLoader(owner: Any): ClassLoader

    /**
     * The locations this application's own bean metadata is read from.
     *
     * Every `META-INF/surf-api/beans` file under one of these URLs belongs to [owner], and nothing
     * else does. A platform must answer this deliberately rather than by delegating to
     * [classLoader], because `ClassLoader.getResources` is parent-first **and additive**: it
     * returns the parents' matches as well as its own. Where surf-api sits above an application on
     * the classpath, that would hand the application surf-api's own bean metadata, and its
     * container would declare every surf-api bean a second time — a second instance of each
     * singleton, with its own `load`, `enable` and `disable`, and nothing to say so, because
     * duplicate detection only looks within one container.
     *
     * A loader that owns its entries can answer with `URLClassLoader.findResources`, which searches
     * its own entries only. A platform whose loader is already own-only may use `getResources`.
     */
    fun beanSources(owner: Any): List<URL>

    /** Whether the platform has a plugin with [pluginId] loaded. */
    fun isPluginLoaded(pluginId: String): Boolean

    /** Where the container reports a skipped bean, a failed phase or an unresolvable parameter. */
    fun logger(owner: Any): PlatformLogger

    /** The directory an application's configuration files live in. */
    fun dataPath(owner: Any): Path

    /** The environments the process runs in, for `@ConditionalOnEnvironment`. */
    fun environments(): Set<String>

    /**
     * Returns the configuration value at [key], or `null` when it is not set.
     *
     * The container never opens a file itself. Configuration formats, their caching and their
     * defaults belong to the platform, and keeping the access behind one function is what lets the
     * condition evaluator be tested without a data directory on disk.
     *
     * @param owner the application whose configuration is read
     * @param key path to the value, segment by segment
     * @param file configuration file relative to [dataPath]; empty means the application's default
     */
    fun property(owner: Any, key: List<String>, file: String): String?
}

/**
 * The little the container needs from a logger.
 *
 * Narrow on purpose: the container must not pull a logging framework into a module that only
 * declares contracts, and every platform already has one to delegate to.
 */
@InternalSurfApi
interface PlatformLogger {

    /** Reports something worth seeing in normal operation. */
    fun info(message: String)

    /** Reports something that will probably surprise someone, such as a skipped bean. */
    fun warn(message: String)

    /** Reports a failure, with the cause when there is one. */
    fun error(message: String, throwable: Throwable? = null)

    /** Reports detail that is only wanted while diagnosing. */
    fun debug(message: String)
}
