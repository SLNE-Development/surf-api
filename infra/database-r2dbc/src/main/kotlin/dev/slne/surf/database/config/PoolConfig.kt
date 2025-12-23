package dev.slne.surf.database.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@ConfigSerializable
internal data class PoolConfig(
    val sizing: Sizing = Sizing(),
    val timeouts: Timeouts = Timeouts()
) {

    @ConfigSerializable
    data class Sizing(
        val initialSize: Int = 10,
        val minIdle: Int = 0,
        val maxSize: Int = 10
    )

    @ConfigSerializable
    data class Timeouts(
        val maxAcquireTimeMillis: Long = 10.seconds.inWholeMilliseconds,
        val maxCreateConnectionTimeMillis: Long = 30.seconds.inWholeMilliseconds,
        val maxValidationTimeMillis: Long = -1,
        val maxIdleTimeMillis: Long = 60.seconds.inWholeMilliseconds,
        val maxLifeTimeMillis: Long = 30.minutes.inWholeMilliseconds
    )
}