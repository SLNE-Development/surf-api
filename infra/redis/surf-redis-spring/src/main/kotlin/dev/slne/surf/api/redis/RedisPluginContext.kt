package dev.slne.surf.api.redis

import dev.slne.surf.api.spring.context.withContextClassLoader
import dev.slne.surf.redis.RedisApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.plus
import org.springframework.beans.factory.getBean
import org.springframework.beans.factory.getBeanProvider
import org.springframework.context.annotation.AnnotationConfigApplicationContext

/**
 * The managed [RedisApi] of one plugin context: creates it on first use and connects it once, right
 * after the context refreshed.
 *
 * The connection opens with surf-api's class loader as thread context class loader, so the Netty
 * and Redisson threads the first connection starts do not pin a plugin's loader.
 */
internal class RedisPluginContext(
    private val context: AnnotationConfigApplicationContext,
    private val pluginName: String,
) {

    @Volatile
    private var connectPointPassed = false

    /**
     * Creates the plugin's [RedisApi], with the contextual serializers of the plugin's
     * [SerializersModule] beans.
     *
     * @throws IllegalStateException if the connect point has passed, because an instance created
     * now would never connect
     */
    fun createApi(): RedisApi {
        check(!connectPointPassed) {
            "RedisApi of plugin '$pluginName' was requested after its context refreshed and would never connect; " +
                "inject it into a bean instead of looking it up later"
        }

        return RedisApi.create(pluginName, serializersModule())
    }

    /**
     * Freezes and connects the plugin's [RedisApi] if a bean created it, and passes the connect
     * point either way.
     *
     * Looks the singleton up without creating it, so a plugin that does not use Redis never opens a
     * connection. An instance that is already frozen or connected is left in that state.
     */
    fun connectIfCreated() {
        connectPointPassed = true
        if (!context.beanFactory.containsSingleton(REDIS_API_BEAN)) return

        val api = context.getBean<RedisApi>(REDIS_API_BEAN)
        runBlocking(Dispatchers.IO) {
            withContextClassLoader(RedisPluginContext::class.java.classLoader) {
                if (!api.isFrozen()) {
                    api.freeze()
                }

                if (!api.isConnected()) {
                    api.connect()
                }
            }
        }
    }

    private fun serializersModule(): SerializersModule {
        return context.getBeanProvider<SerializersModule>()
            .orderedStream()
            .reduce(EmptySerializersModule()) { combined, module -> combined + module }
    }
}
