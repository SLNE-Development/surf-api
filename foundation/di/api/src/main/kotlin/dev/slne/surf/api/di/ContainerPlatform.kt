package dev.slne.surf.api.di

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import java.nio.file.Path

/**
 * The four things the container cannot work out for itself and has to ask the platform.
 *
 * The container is plain JVM logic: it resolves types, orders a graph and drives a lifecycle. Where
 * the bean classes are loaded from, whether a plugin is present, where to log and where
 * configuration lives are all platform questions, and they are the only ones. A platform
 * contributes an implementation of this interface and the rest of the container runs unchanged —
 * which is also what lets the tests drive it without a server.
 *
 * Passed to the container at construction rather than inherited from, so a test can supply a
 * different one per container.
 */
@InternalSurfApi
interface ContainerPlatform {

    /**
     * The classloader whose `META-INF/surf-api/beans` files make up the container's beans, and
     * which loads their classes.
     *
     * On a platform that isolates applications from each other this is that application's own
     * loader, which is exactly why two applications get two containers.
     */
    fun classLoader(owner: Any): ClassLoader

    /** Whether the platform has a plugin with [pluginId] loaded. */
    fun isPluginLoaded(pluginId: String): Boolean

    /** Where the container reports a skipped bean, a failed phase or an unresolvable parameter. */
    fun logger(owner: Any): PlatformLogger

    /** The directory an application's configuration files live in. */
    fun dataPath(owner: Any): Path

    /** The environments the process runs in, for `@ConditionalOnEnvironment`. */
    fun environments(): Set<String>
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
