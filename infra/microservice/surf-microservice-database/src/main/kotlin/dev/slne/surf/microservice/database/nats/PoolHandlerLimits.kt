package dev.slne.surf.microservice.database.nats

/**
 * NATS handler limits per service registration derived from the size of the database pool.
 *
 * [total] is twice the pool size, so handlers that wait for a connection keep the pool busy without queueing
 * unboundedly in front of it. [long] is half the pool size, at least one, so long-running calls never hold the whole
 * pool.
 */
internal data class PoolHandlerLimits(val total: Int, val long: Int) {

    companion object {

        /**
         * Returns the limits for a pool of at most [poolMaxSize] connections.
         *
         * @throws IllegalArgumentException when [poolMaxSize] is not positive
         */
        fun forPoolSize(poolMaxSize: Int): PoolHandlerLimits {
            require(poolMaxSize > 0) { "Pool size must be positive, was $poolMaxSize" }
            return PoolHandlerLimits(total = 2 * poolMaxSize, long = (poolMaxSize / 2).coerceAtLeast(1))
        }
    }
}
