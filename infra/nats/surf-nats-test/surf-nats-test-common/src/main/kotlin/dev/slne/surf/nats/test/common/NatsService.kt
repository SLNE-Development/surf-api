@file:OptIn(InternalNatsApi::class)

package dev.slne.surf.nats.test.common

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.NatsClient
import dev.slne.surf.nats.internal.requiredService
import org.jetbrains.annotations.MustBeInvokedByOverriders

/**
 * Owns the [NatsClient] of a surf-nats test application and registers its listeners and services.
 *
 * Each test application provides exactly one implementation through [java.util.ServiceLoader], usually with
 * `@AutoService(NatsService::class)`, and overrides [start] to register its listeners and services on [client].
 * [NatsTestInstance] calls [start] and [stop].
 */
abstract class NatsService {

    /**
     * The client named `surf-nats-test`, created when the service is constructed.
     */
    val client = NatsClient.create("surf-nats-test")

    /**
     * Registers the listeners and services of the test application on [client].
     */
    @MustBeInvokedByOverriders
    open suspend fun start() {

    }

    /**
     * Closes [client], which ends all its registrations.
     */
    fun stop() {
        client.close()
    }

    companion object {
        /**
         * The implementation loaded through [java.util.ServiceLoader] when this companion is first accessed.
         *
         * Accessing it throws [java.util.ServiceConfigurationError] when no implementation is registered.
         */
        val instance = requiredService<NatsService>()

        /**
         * Returns [instance].
         */
        fun get() = instance
    }
}

/**
 * Gets the [NatsService.client] of the registered [NatsService].
 */
val natsClient get() = NatsService.get().client