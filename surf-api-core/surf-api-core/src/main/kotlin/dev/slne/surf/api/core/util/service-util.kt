package dev.slne.surf.api.core.util

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
inline fun <reified T> requiredService(): T = Services.serviceWithFallback(T::class.java)
    .orElseThrow { ServiceConfigurationError("Service ${T::class.java.name} not available") }