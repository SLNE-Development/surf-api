package dev.slne.surf.database.config.internal

import dev.slne.surf.database.config.DatabaseConfig
import io.r2dbc.pool.ConnectionPool
import io.r2dbc.pool.ConnectionPoolConfiguration
import reactor.netty.resources.LoopResources
import java.time.Duration.ofMillis

/**
 * An r2dbc-pool [ConnectionPool] with an event loop of its own, built from a [DatabaseConfig].
 *
 * [close] disposes the pool, then the event loop.
 */
internal class PooledConnections private constructor(
    val pool: ConnectionPool,
    private val loopResources: LoopResources,
) : AutoCloseable {

    override fun close() {
        try {
            pool.dispose()
        } finally {
            loopResources.dispose()
        }
    }

    companion object {

        /**
         * Builds the pool for [config]; [name] names the pool and its event loop threads.
         */
        fun open(config: DatabaseConfig, name: String): PooledConnections {
            val details = config.credentials
            val timeouts = config.pool.timeouts
            val sizing = config.pool.sizing
            val loopResources = LoopResources.create("${details.databaseType.vendor}-r2dbc-event-loop-$name", 1, 4, true)

            val connectionFactory = R2dbcConnectionFactories.create(details, ofMillis(timeouts.lockTimeoutMillis), loopResources)
            val poolConfiguration = ConnectionPoolConfiguration.builder()
                .connectionFactory(connectionFactory)
                // A retry would restart the acquire timeout at the end of the queue
                .acquireRetry(0)
                .initialSize(sizing.initialSize)
                .minIdle(sizing.minIdle)
                .maxSize(sizing.maxSize)
                .maxAcquireTime(ofMillis(timeouts.maxAcquireTimeMillis))
                .maxCreateConnectionTime(ofMillis(timeouts.maxCreateConnectionTimeMillis))
                .maxIdleTime(ofMillis(timeouts.maxIdleTimeMillis))
                .maxLifeTime(ofMillis(timeouts.maxLifeTimeMillis))
                .maxValidationTime(ofMillis(timeouts.maxValidationTimeMillis))
                .name(name)
                .build()

            return PooledConnections(ConnectionPool(poolConfiguration), loopResources)
        }
    }
}
