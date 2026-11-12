package dev.slne.surf.microservice.database.nats

import dev.slne.surf.nats.NatsHandlerLimits

/**
 * Sets the surf-nats handler limit defaults.
 *
 * The only class that links against surf-nats. Load it only after [isSurfNatsPresent] returned `true`.
 */
internal object NatsHandlerLimitsSetter {

    fun setDefaults(limits: PoolHandlerLimits) {
        NatsHandlerLimits.setDefaults(limits.total, limits.long)
    }
}
