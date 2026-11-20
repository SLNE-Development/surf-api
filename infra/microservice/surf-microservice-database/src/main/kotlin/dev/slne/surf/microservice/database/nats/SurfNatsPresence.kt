package dev.slne.surf.microservice.database.nats

import org.springframework.util.ClassUtils

private const val NATS_HANDLER_LIMITS_CLASS = "dev.slne.surf.nats.NatsHandlerLimits"

/**
 * Returns whether surf-nats is on the class path of the microservice.
 */
internal fun isSurfNatsPresent(): Boolean =
    ClassUtils.isPresent(NATS_HANDLER_LIMITS_CLASS, PoolHandlerLimits::class.java.classLoader)
