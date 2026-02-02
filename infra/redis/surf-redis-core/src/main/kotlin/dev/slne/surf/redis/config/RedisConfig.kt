package dev.slne.surf.redis.config

import dev.slne.surf.redis.RedisInstance
import dev.slne.surf.surfapi.core.api.config.SpongeYmlConfigClass
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import java.util.*

@ConfigSerializable
data class RedisConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String? = null,
    val clientName: String = "surf-redis-client-${UUID.randomUUID()}",
) {
    companion object : SpongeYmlConfigClass<RedisConfig>(
        RedisConfig::class.java,
        RedisInstance.instance.dataPath,
        "config.yml"
    )
}