package dev.slne.surf.nats

inline fun <reified S : Any> NatsClient.service(): S = service(S::class)

inline fun <reified S : Any> NatsClient.registerService(implementation: S): NatsRegistration {
    return registerService(S::class, implementation)
}