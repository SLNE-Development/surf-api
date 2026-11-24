package dev.slne.surf.api.redis

import java.lang.reflect.InvocationHandler
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Proxy

/**
 * Builds the proxies behind the `RedissonClient` and `RedissonReactiveClient` beans of a plugin.
 */
internal object RedisClientProxy {

    private val NO_ARGUMENTS = emptyArray<Any?>()

    /**
     * Returns a proxy of [type] that forwards every call to the client [target] returns.
     *
     * A call while [isConnected] is `false` fails with [IllegalStateException], which covers the
     * time before the connection opens and the time after it closed, when the underlying client
     * would otherwise be stale. [target] is read per call and only while connected. The proxy
     * refuses `shutdown`, because the client is shared by every plugin and owned by surf-api.
     * `equals`, `hashCode` and `toString` never reach the client.
     */
    fun <T : Any> create(
        type: Class<T>,
        pluginName: String,
        isConnected: () -> Boolean,
        target: () -> T,
    ): T {
        val handler = InvocationHandler { proxy, method, arguments ->
            when {
                method.declaringClass == Any::class.java -> invokeObjectMethod(
                    proxy,
                    method.name,
                    arguments,
                    type,
                    pluginName,
                )

                method.name == "shutdown" -> throw UnsupportedOperationException(
                    "The Redisson client is shared and owned by surf-api; plugin '$pluginName' must not shut it down",
                )

                !isConnected() -> throw IllegalStateException(
                    "Redis is not connected yet for plugin $pluginName",
                )

                else -> forward(target(), method, arguments)
            }
        }

        return type.cast(Proxy.newProxyInstance(type.classLoader, arrayOf(type), handler))
    }

    private fun invokeObjectMethod(
        proxy: Any,
        name: String,
        arguments: Array<Any?>?,
        type: Class<*>,
        pluginName: String,
    ): Any {
        return when (name) {
            "equals" -> proxy === arguments?.get(0)
            "hashCode" -> System.identityHashCode(proxy)
            "toString" -> "${type.simpleName} proxy of plugin $pluginName"
            else -> throw MatchException("Unknown Object method '$name'", null)
        }
    }

    private fun forward(target: Any, method: Method, arguments: Array<Any?>?): Any? {
        return try {
            method.invoke(target, *(arguments ?: NO_ARGUMENTS))
        } catch (failure: InvocationTargetException) {
            throw failure.targetException
        }
    }
}
