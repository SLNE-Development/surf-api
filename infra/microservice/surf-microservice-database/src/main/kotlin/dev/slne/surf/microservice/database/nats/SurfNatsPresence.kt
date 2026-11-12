package dev.slne.surf.microservice.database.nats

private const val NATS_HANDLER_LIMITS_CLASS = "dev.slne.surf.nats.NatsHandlerLimits"

/**
 * Returns whether surf-nats is on the class path of the microservice.
 */
internal fun isSurfNatsPresent(): Boolean = try {
    Class.forName(NATS_HANDLER_LIMITS_CLASS, false, PoolHandlerLimits::class.java.classLoader)
    true
} catch (_: ClassNotFoundException) {
    false
}
