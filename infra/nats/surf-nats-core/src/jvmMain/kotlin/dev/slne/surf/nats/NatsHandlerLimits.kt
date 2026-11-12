package dev.slne.surf.nats

import dev.slne.surf.nats.config.HandlerLimits
import dev.slne.surf.nats.rpc.RpcDefaults

/**
 * Holds the default handler limits of service registrations, set by the application that embeds surf-nats.
 *
 * Each service registration runs at most a limited number of handlers at the same time. Requests that find every
 * slot busy wait and are admitted earliest deadline first. Long-running calls may hold only part of the slots, so
 * they cannot block short calls. Without a call to [setDefaults], a registration admits 20 handlers, 10 of them
 * long-running.
 *
 * The NATS `config.yml` takes precedence over these defaults, in this order:
 *
 * 1. `max-concurrent-handlers-by-service` and `max-concurrent-long-handlers-by-service`, keyed by the proto full name
 *    of the service.
 * 2. `max-concurrent-handlers` and `max-concurrent-long-handlers`, which the environment variables
 *    `SURF_NATS_MAX_CONCURRENT_HANDLERS` and `SURF_NATS_MAX_CONCURRENT_LONG_HANDLERS` override.
 * 3. The defaults set here.
 *
 * A configured total without a configured long limit gets half of that total, at least one, as its long limit, and
 * ignores the long default set here.
 *
 * A service registration reads its limits once, when it is registered, so set the defaults before any service is
 * registered. Thread-safe.
 *
 * To size the limits to a database pool of 10 connections, call:
 *
 * ```kotlin
 * NatsHandlerLimits.setDefaults(maxConcurrentHandlers = 10, maxConcurrentLongHandlers = 5)
 * ```
 */
object NatsHandlerLimits {

    @Volatile
    internal var defaults = HandlerLimits(RpcDefaults.MAX_CONCURRENT_HANDLERS)
        private set

    /**
     * Sets the default limits: at most [maxConcurrentHandlers] handlers per service registration run at the same
     * time, of which at most [maxConcurrentLongHandlers] are long-running calls.
     *
     * Long-running calls are fire calls, fan-outs and calls of methods whose timeout exceeds the core default of
     * 5 seconds. [maxConcurrentLongHandlers] is clamped to `1..maxConcurrentHandlers`, so a share derived by integer
     * division, such as half of a pool of one, still admits long-running calls.
     *
     * @param maxConcurrentHandlers The maximum number of handlers of one service registration that run at the same
     * time.
     * @param maxConcurrentLongHandlers The maximum number of those handlers that serve long-running calls. Defaults
     * to half of [maxConcurrentHandlers], at least one.
     * @throws IllegalArgumentException when [maxConcurrentHandlers] is not positive
     */
    fun setDefaults(
        maxConcurrentHandlers: Int,
        maxConcurrentLongHandlers: Int = HandlerLimits.defaultLong(maxConcurrentHandlers),
    ) {
        require(maxConcurrentHandlers > 0) { "Handler limit must be positive, was $maxConcurrentHandlers" }
        defaults = HandlerLimits(maxConcurrentHandlers, maxConcurrentLongHandlers.coerceIn(1, maxConcurrentHandlers))
    }
}
