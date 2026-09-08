package dev.slne.surf.redis.internal

import org.redisson.api.RedissonClient
import org.redisson.api.RedissonReactiveClient
import org.redisson.codec.BaseEventCodec
import org.redisson.config.Config
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

internal class FakeRedisson {
    private val shutdowns = AtomicInteger()

    val shutdownCount: Int get() = shutdowns.get()
    val isShutdown: Boolean get() = shutdowns.get() > 0

    val reactive: RedissonReactiveClient = proxy(RedissonReactiveClient::class.java) { method, _ ->
        throw UnsupportedOperationException("FakeRedisson.reactive.${method.name}")
    }

    val client: RedissonClient = proxy(RedissonClient::class.java) { method, _ ->
        when (method.name) {
            "shutdown" -> shutdowns.incrementAndGet().let { null }
            "isShutdown", "isShuttingDown" -> isShutdown
            "reactive" -> reactive
            else -> throw UnsupportedOperationException("FakeRedisson.${method.name}")
        }
    }

    private fun <T> proxy(type: Class<T>, handler: (java.lang.reflect.Method, Array<Any?>?) -> Any?): T {
        val instance = Proxy.newProxyInstance(type.classLoader, arrayOf(type)) { self, method, args ->
            when (method.name) {
                "toString" -> "Fake${type.simpleName}@${System.identityHashCode(self)}"
                "hashCode" -> System.identityHashCode(self)
                "equals" -> args!![0] === self
                else -> handler(method, args)
            }
        }
        @Suppress("UNCHECKED_CAST")
        return instance as T
    }
}

internal class FakeRedissonConnector : RedissonConnector {
    val created = CopyOnWriteArrayList<FakeRedisson>()

    @Volatile
    var nextConnectFailure: Throwable? = null

    @Volatile
    var nextProbeFailure: Throwable? = null

    override fun connect(config: Config): RedissonClient {
        nextConnectFailure?.let {
            nextConnectFailure = null
            throw it
        }
        return FakeRedisson().also(created::add).client
    }

    override fun detectOsType(redisson: RedissonClient): BaseEventCodec.OSType? {
        nextProbeFailure?.let {
            nextProbeFailure = null
            throw it
        }
        return null
    }

    fun fakeOf(client: RedissonClient): FakeRedisson = created.first { it.client === client }

    val totalShutdowns: Int get() = created.sumOf { it.shutdownCount }
}
