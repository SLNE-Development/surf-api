@file:OptIn(InternalNatsApi::class)

package dev.slne.surf.nats.test.common

import dev.slne.surf.nats.InternalNatsApi
import dev.slne.surf.nats.internal.requiredService
import org.jetbrains.annotations.MustBeInvokedByOverriders

/**
 * Drives the lifecycle of a surf-nats test application from its platform entry point.
 *
 * Each test application provides exactly one implementation through [java.util.ServiceLoader], usually with
 * `@AutoService(NatsTestInstance::class)`. The entry point of the test application calls [load], [enable] and
 * [disable] in this order.
 */
abstract class NatsTestInstance {

    /**
     * Starts the registered [NatsService], which registers its listeners and services.
     */
    @MustBeInvokedByOverriders
    open suspend fun load() {
        NatsService.get().start()
    }

    /**
     * Runs after [load] once the application is enabled. Does nothing by default.
     */
    @MustBeInvokedByOverriders
    open suspend fun enable() {
    }

    /**
     * Stops the registered [NatsService], which closes its client.
     */
    @MustBeInvokedByOverriders
    open suspend fun disable() {
        NatsService.get().stop()
    }

    companion object {
        /**
         * The implementation loaded through [java.util.ServiceLoader] when this companion is first accessed.
         *
         * Accessing it throws [java.util.ServiceConfigurationError] when no implementation is registered.
         */
        val instance = requiredService<NatsTestInstance>()

        /**
         * Returns [instance].
         */
        fun get() = instance
    }
}