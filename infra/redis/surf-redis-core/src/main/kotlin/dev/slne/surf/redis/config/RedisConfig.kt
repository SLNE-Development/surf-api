package dev.slne.surf.redis.config

import dev.slne.surf.api.core.config.SpongeYmlConfigClass
import dev.slne.surf.redis.RedisInstance
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*

@ConfigSerializable
data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String? = null,
    val clientName: String = "surf-redis-client-${UUID.randomUUID()}",
) {
    fun overwriteFromEnv() = copy(
        host = RedisEnvironment.SURF_REDIS_HOST ?: host,
        port = RedisEnvironment.SURF_REDIS_PORT ?: port,
        password = RedisEnvironment.SURF_REDIS_PASSWORD ?: password,
        clientName = RedisEnvironment.SURF_REDIS_CLIENT_NAME ?: clientName
    )

    companion object : SpongeYmlConfigClass<RedisConfig>(
        RedisConfig::class.java,
        RedisInstance.instance.dataPath,
        "config.yml"
    )
}

val redisConfig by lazy { RedisConfig.getConfig().overwriteFromEnv() }