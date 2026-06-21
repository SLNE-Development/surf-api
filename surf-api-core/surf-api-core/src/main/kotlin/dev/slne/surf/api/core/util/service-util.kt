package dev.slne.surf.api.core.util

import net.kyori.adventure.internal.properties.AdventureProperties
import net.kyori.adventure.util.Services
import java.util.*

/**
 * Retrieves a required service of type [T] using the Adventure service provider mechanism.
 *
 * This function uses `Services.serviceWithFallback` to locate a service implementation.
 * If no service provider is registered for the specified type, an exception is thrown.
 *
 * @return the service instance of type [T]
 * @throws ServiceConfigurationError if the service of type [T] is not available
 */
inline fun <reified T : Any> requiredService(): T = ServiceUtil.serviceWithFallback<T>(
    ServiceLoader.load(
        T::class.java,
        getCallerClass(-1)?.classLoader ?: T::class.java.classLoader
    ), T::class.java
) ?: throw ServiceConfigurationError("Service ${T::class.java.name} not available")

object ServiceUtil {
    @Suppress("UnstableApiUsage")
    private val SERVICE_LOAD_FAILURES_ARE_FATAL = AdventureProperties.SERVICE_LOAD_FAILURES_ARE_FATAL.value() == true

    @PublishedApi
    internal fun <T : Any> serviceWithFallback(loader: ServiceLoader<T>, type: Class<T>): T? {
        val iterator = loader.iterator()
        var firstFallback: T? = null

        while (iterator.hasNext()) {
            try {
                val next = iterator.next()
                if (next is Services.Fallback) {
                    if (firstFallback == null) {
                        firstFallback = next
                    }
                } else {
                    return next
                }
            } catch (t: Throwable) {
                if (SERVICE_LOAD_FAILURES_ARE_FATAL) {
                    throw ServiceConfigurationError("Failed to load service $type", t)
                } else {
                    continue
                }
            }
        }

        return firstFallback
    }
}