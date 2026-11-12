package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import java.util.*

/**
 * Loads the first [ServiceLoader] implementation of [T] from the class loader of [T].
 *
 * Stands in for the `requiredService` lookup that surf-api 4 removed, until surf-nats moves to
 * dependency injection.
 *
 * @throws ServiceConfigurationError if no implementation of [T] is registered
 */
@InternalNatsApi
public inline fun <reified T : Any> requiredService(): T =
    ServiceLoader.load(T::class.java, T::class.java.classLoader).firstOrNull()
        ?: throw ServiceConfigurationError("Service ${T::class.java.name} not available")
