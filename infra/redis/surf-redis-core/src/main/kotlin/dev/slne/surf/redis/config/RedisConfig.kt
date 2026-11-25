package dev.slne.surf.redis.config

import dev.slne.surf.api.config.ConfigHandles
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
    fun overwriteFromEnv(): RedisConfig {
        return copy(
            host = RedisEnvironment.SURF_REDIS_HOST ?: host,
            port = RedisEnvironment.SURF_REDIS_PORT ?: port,
            password = RedisEnvironment.SURF_REDIS_PASSWORD ?: password,
            clientName = RedisEnvironment.SURF_REDIS_CLIENT_NAME ?: clientName,
        )
    }

    companion object {

        /**
         * Reads `config.yml` in [RedisInstance.dataPath], writing it with its defaults if it is
         * missing, and applies the environment overrides.
         *
         * @return the settings
         */
        fun load(): RedisConfig {
            val file = RedisInstance.instance.dataPath.resolve(FILE_NAME)
            val loaded = ConfigHandles.open<RedisConfig>(file).use { handle -> handle.value }

            return loaded.overwriteFromEnv()
        }

        private const val FILE_NAME = "config.yml"
    }
}

val redisConfig by lazy { RedisConfig.load() }
