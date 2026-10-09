@file:MustUseReturnValues
package dev.slne.surf.nats

import org.jetbrains.annotations.Blocking
import org.jetbrains.annotations.CheckReturnValue

@CheckReturnValue
inline fun <reified S : Any> NatsClient.service(): S = service(S::class)

@Blocking
@IgnorableReturnValue
inline fun <reified S : Any> NatsClient.registerService(implementation: S): NatsRegistration {
    return registerService(S::class, implementation)
}