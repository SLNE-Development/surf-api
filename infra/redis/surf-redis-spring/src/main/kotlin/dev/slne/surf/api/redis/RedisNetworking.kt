package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.SurfLifecycle
import dev.slne.surf.redis.RedisInstance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.getBean
import org.springframework.stereotype.Component

/**
 * Stops the process-wide Redis networking when surf-api shuts down.
 *
 * A bean of the root context, so its `onDisable` runs after every plugin context closed and
 * disconnected its `RedisApi`. It stops the networking only if the lazy [RedisInstance] bean was
 * ever created.
 */
@Component
internal class RedisNetworking(
    private val beanFactory: ConfigurableListableBeanFactory,
) : SurfLifecycle {

    override suspend fun onDisable() {
        if (!beanFactory.containsSingleton(REDIS_INSTANCE_BEAN)) return

        val instance = beanFactory.getBean<RedisInstance>(REDIS_INSTANCE_BEAN)
        withContext(Dispatchers.IO) {
            instance.disable()
        }
    }
}
