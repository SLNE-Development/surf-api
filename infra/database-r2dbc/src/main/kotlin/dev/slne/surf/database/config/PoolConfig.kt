package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.meta.Comment
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * Sizing and timeouts of the connection pool.
 */
@ConfigSerializable
data class PoolConfig(
    val sizing: Sizing = Sizing(),
    val timeouts: Timeouts = Timeouts()
) {

    /**
     * The number of connections the pool keeps.
     */
    @ConfigSerializable
    data class Sizing(
        val initialSize: Int = 10,
        val minIdle: Int = 0,
        val maxSize: Int = 10
    )

    /**
     * Timeouts of the pool and of its connections, in milliseconds.
     *
     * @property lockTimeoutMillis how long a statement waits for a row lock before it fails, in `1..Int.MAX_VALUE` (the
     * range of PostgreSQL's `lock_timeout`); MariaDB rounds it up to whole seconds
     */
    @ConfigSerializable
    data class Timeouts(
        val maxAcquireTimeMillis: Long = 5.seconds.inWholeMilliseconds,
        val maxCreateConnectionTimeMillis: Long = 5.seconds.inWholeMilliseconds,
        val maxValidationTimeMillis: Long = -1,
        val maxIdleTimeMillis: Long = 60.seconds.inWholeMilliseconds,
        val maxLifeTimeMillis: Long = 30.minutes.inWholeMilliseconds,
        @param:Comment("How long a statement waits for a row lock before it fails. MariaDB rounds it up to whole seconds.")
        val lockTimeoutMillis: Long = 10.seconds.inWholeMilliseconds,
    )
}