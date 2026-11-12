package dev.slne.surf.microservice.database.nats

import dev.slne.surf.api.common.util.logger

private val log = logger()

/**
 * Sizes the default NATS handler limits to the database pool, see [PoolHandlerLimits].
 *
 * The limits are surf-nats defaults: `max-concurrent-handlers` and `max-concurrent-long-handlers` in the NATS
 * `config.yml`, their `-by-service` entries and their environment variables take precedence. Nothing changes when
 * surf-nats is absent or the pool size is unknown.
 */
internal class PoolSizedHandlerLimits(
    private val isNatsPresent: () -> Boolean = ::isSurfNatsPresent,
    private val setDefaults: (PoolHandlerLimits) -> Unit = { limits -> NatsHandlerLimitsSetter.setDefaults(limits) },
) {

    /**
     * Applies the limits for a pool of at most [poolMaxSize] connections. `null` leaves the defaults unchanged.
     *
     * A surf-nats version without a matching `NatsHandlerLimits.setDefaults` keeps its own defaults and logs a
     * warning. The limits only tune throughput, so they never fail the start.
     */
    fun apply(poolMaxSize: Int?) {
        if (poolMaxSize == null || !isNatsPresent()) return

        val limits = PoolHandlerLimits.forPoolSize(poolMaxSize)
        try {
            setDefaults(limits)
        } catch (error: LinkageError) {
            log.atWarning()
                .withCause(error)
                .log("Keeping the NATS handler limit defaults: this surf-nats version cannot set them")
            return
        }

        log.atInfo()
            .log(
                "NATS handlers per service default to %d, %d of them long-running, for a database pool of %d",
                limits.total,
                limits.long,
                poolMaxSize,
            )
    }
}
