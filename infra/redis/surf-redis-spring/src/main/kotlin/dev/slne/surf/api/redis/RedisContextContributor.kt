package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.context.ContextContributor
import dev.slne.surf.api.spring.context.PluginDescriptor
import dev.slne.surf.redis.RedisApi
import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.springframework.beans.factory.SmartInitializingSingleton
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.support.RootBeanDefinition
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.stereotype.Component

internal const val REDIS_API_BEAN = "surfRedisApi"
internal const val REDISSON_CLIENT_BEAN = "surfRedissonClient"
internal const val REDISSON_REACTIVE_CLIENT_BEAN = "surfRedissonReactiveClient"
internal const val REDIS_CONNECTOR_BEAN = "surfRedisConnector"

/**
 * Gives every plugin context its Redis beans.
 *
 * - `RedisApi`, created by `RedisApi.create(pluginName, serializersModule)` when a bean first asks
 *   for it, where `serializersModule` merges the context's `SerializersModule` beans; disconnected
 *   when the context closes. It is a lazy fallback bean, so a plugin's own `RedisApi` bean wins.
 * - `RedissonClient` and `RedissonReactiveClient`, lazy fallback proxies that forward to the
 *   plugin's `RedisApi` once it is connected and fail with [IllegalStateException] before and
 *   after. The `RedisApi` is resolved by type, so it is the plugin's own bean when it declares one;
 *   creating a proxy creates it.
 * - A connector that connects the `RedisApi`, if one was created, right after the context
 *   refreshed and before any `onLoad` hook, see [RedisPluginContext].
 *
 * A plugin that never injects any of these never starts the Redis networking.
 */
@Component
internal class RedisContextContributor : ContextContributor {

    override fun contribute(context: AnnotationConfigApplicationContext, plugin: PluginDescriptor) {
        val redis = RedisPluginContext(context, plugin.name)

        context.registerBeanDefinition(REDIS_API_BEAN, redisApiDefinition(redis))
        context.registerBeanDefinition(
            REDISSON_CLIENT_BEAN,
            clientDefinition<RedissonClient>(
                context,
                plugin.name,
            ) { redisApi -> redisApi.redisson }
        )
        context.registerBeanDefinition(
            REDISSON_REACTIVE_CLIENT_BEAN,
            clientDefinition<RedissonReactiveClient>(
                context,
                plugin.name,
            ) { redisApi -> redisApi.redissonReactive }
        )
        context.registerBeanDefinition(REDIS_CONNECTOR_BEAN, connectorDefinition(redis))
    }

    private fun redisApiDefinition(redis: RedisPluginContext): RootBeanDefinition {
        return RootBeanDefinition(RedisApi::class.java).apply {
            isLazyInit = true
            isFallback = true
            setInstanceSupplier { redis.createApi() }
            destroyMethodName = "disconnect"
        }
    }

    private inline fun <reified T : Any> clientDefinition(
        context: AnnotationConfigApplicationContext,
        pluginName: String,
        crossinline client: (RedisApi) -> T,
    ) = RootBeanDefinition(T::class.java).apply {
        isLazyInit = true
        isFallback = true
        setInstanceSupplier {
            val api = context.getBean<RedisApi>()
            RedisClientProxy.create(
                type = T::class.java,
                pluginName = pluginName,
                isConnected = api::isConnected,
                target = { client(api) },
            )
        }
    }

    private fun connectorDefinition(redis: RedisPluginContext): RootBeanDefinition {
        return RootBeanDefinition(RedisConnector::class.java) { RedisConnector(redis) }
    }
}

/**
 * Connects the plugin's managed `RedisApi` once every singleton of the context exists, which is
 * inside the context's refresh.
 *
 * A failing connection fails the refresh, so the plugin does not load, and closing the context
 * releases what the instance held.
 */
internal class RedisConnector(private val redis: RedisPluginContext) : SmartInitializingSingleton {

    override fun afterSingletonsInstantiated() {
        redis.connectIfCreated()
    }
}
