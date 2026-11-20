package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.redis.RedisInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.stereotype.Component

/**
 * Stops the process-wide Redis networking when surf-api shuts down.
 *
 * A bean of the root context, so its `onDisable` runs after every plugin context closed and
 * disconnected its `RedisApi`. It stops the networking only if a plugin ever started it.
 */
@Component
internal class RedisNetworking : SurfLifecycle {

    override suspend fun onDisable() {
        withContext(Dispatchers.IO) {
            RedisInstance.disableIfStarted()
        }
    }
}
