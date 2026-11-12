package dev.slne.surf.redis.config

import dev.slne.surf.api.core.environment.env

object RedisEnvironment {
    val SURF_REDIS_HOST by env.optional()
    val SURF_REDIS_PORT by env.optionalInt {
        require("Port must be between 0 and 65535") { it in 0..65535 }
    }
    val SURF_REDIS_PASSWORD by env.optional(sensitive = true)
    val SURF_REDIS_CLIENT_NAME by env.optional()
}