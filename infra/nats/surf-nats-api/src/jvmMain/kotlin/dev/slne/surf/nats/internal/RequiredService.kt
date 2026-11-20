package dev.slne.surf.nats.internal

import dev.slne.surf.nats.InternalNatsApi
import java.util.*

/**
 * Loads the first [ServiceLoader] implementation of [T] from the class loader of [T].
 *
 * surf-nats finds its internal implementations through this seam instead of a Spring context,
 * so the lookup also works in standalone processes that run without one.
 *
 * @throws ServiceConfigurationError if no implementation of [T] is registered
 */
@InternalNatsApi
public inline fun <reified T : Any> requiredService(): T =
    ServiceLoader.load(T::class.java, T::class.java.classLoader).firstOrNull()
        ?: throw ServiceConfigurationError("Service ${T::class.java.name} not available")
