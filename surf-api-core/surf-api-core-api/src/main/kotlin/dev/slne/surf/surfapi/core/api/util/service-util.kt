package dev.slne.surf.surfapi.core.api.util

import net.kyori.adventure.util.Services

/**
 * Retrieves a required service of the specified type using `Services.serviceWithFallback`.
 * If the service is not available, it throws an `Error`.
 *
 * @return The service instance of the specified type `T` if available.
 * @throws Error if the service of type `T` is not available.
 */
inline fun <reified T> requiredService(): T = Services.serviceWithFallback(T::class.java)
    .orElseThrow { Error("Service ${T::class.java.name} not available") }