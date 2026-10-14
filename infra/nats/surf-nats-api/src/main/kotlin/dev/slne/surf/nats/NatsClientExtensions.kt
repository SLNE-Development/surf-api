@file:MustUseReturnValues
package dev.slne.surf.nats

import org.jetbrains.annotations.CheckReturnValue

/**
 * Returns the proxy for [S] using [NatsClient.service].
 */
@CheckReturnValue
public inline fun <reified S : Any> NatsClient.service(): S = service(S::class)

/**
 * Registers [implementation] as service [S] using [NatsClient.registerService].
 */
@IgnorableReturnValue
public suspend inline fun <reified S : Any> NatsClient.registerService(implementation: S): NatsRegistration {
    return registerService(S::class, implementation)
}