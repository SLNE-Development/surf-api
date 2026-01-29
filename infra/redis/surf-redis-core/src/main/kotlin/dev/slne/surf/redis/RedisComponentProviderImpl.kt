package dev.slne.surf.redis

import com.google.auto.service.AutoService
import dev.slne.surf.redis.cache.*
import dev.slne.surf.redis.event.RedisEventBus
import dev.slne.surf.redis.event.RedisEventBusImpl
import dev.slne.surf.redis.internal.RedissonConfigDetails
import dev.slne.surf.redis.request.RequestResponseBus
import dev.slne.surf.redis.request.RequestResponseBusImpl
import dev.slne.surf.redis.sync.list.SyncList
import dev.slne.surf.redis.sync.list.SyncListImpl
import dev.slne.surf.redis.sync.map.SyncMap
import dev.slne.surf.redis.sync.map.SyncMapImpl
import dev.slne.surf.redis.sync.set.SyncSet
import dev.slne.surf.redis.sync.set.SyncSetImpl
import dev.slne.surf.redis.sync.value.SyncValue
import dev.slne.surf.redis.sync.value.SyncValueImpl
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelDuplexHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.epoll.Epoll
import io.netty.handler.logging.LogLevel
import io.netty.handler.logging.LoggingHandler
import io.netty.handler.timeout.IdleStateHandler
import kotlinx.serialization.KSerializer
import org.redisson.client.NettyHook
import org.redisson.config.Config
import org.redisson.config.EqualJitterDelay
import org.redisson.config.TransportMode
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

@AutoService(RedisComponentProvider::class)
class RedisComponentProviderImpl : RedisComponentProvider {
    private val transportMode = if (Epoll.isAvailable()) {
        TransportMode.EPOLL
    } else {
        TransportMode.NIO
    }

    override val eventLoopGroup get() = RedisInstance.instance.eventLoopGroup
    override val redissonExecutorService get() = RedisInstance.instance.redissonExecutorService
    override val clientId = UUID.randomUUID().toString()
        .split("-")
        .take(2)
        .joinToString("")

    override fun createRedissonConfig(details: RedissonConfigDetails): Config {
        val config = Config()
            .setPassword(details.redisURI.password)
            .setTransportMode(transportMode)
            .setTcpKeepAlive(true)
            .setTcpNoDelay(true)
            .setTcpKeepAliveInterval(5.seconds.inWholeMilliseconds.toInt())
            .setEventLoopGroup(eventLoopGroup)
            .setExecutor(redissonExecutorService)
            .apply {
                useSingleServer()
                    .setPingConnectionInterval(10.seconds.inWholeMilliseconds.toInt())
                    .setConnectTimeout(5.seconds.inWholeMilliseconds.toInt())
                    .setRetryAttempts(10)
                    .setRetryDelay(EqualJitterDelay(200.milliseconds.toJavaDuration(), 1.seconds.toJavaDuration()))
                    .setAddress(details.redisURI.toString())
            }

        config.setNettyHook(object : NettyHook {
            override fun afterBoostrapInitialization(bootstrap: Bootstrap) {

            }

            override fun afterChannelInitialization(channel: Channel) {
                val id = channel.id().asShortText()
                val remote = channel.remoteAddress()
                val local = channel.localAddress()

//                channel.pipeline().addFirst("netty-logger", LoggingHandler(LogLevel.INFO))
                channel.pipeline().addFirst("idle", IdleStateHandler(30, 30, 0, TimeUnit.SECONDS))

                channel.pipeline().addLast("redis-conn-debug", object : ChannelDuplexHandler() {
                    private fun meta(ctx: ChannelHandlerContext): String {
                        val ch = ctx.channel()
                        return "id=${ch.id().asShortText()} " +
                                "ch=${ch.javaClass.simpleName} " +
                                "active=${ch.isActive} open=${ch.isOpen} " +
                                "local=${ch.localAddress()} remote=${ch.remoteAddress()}"
                    }

                    override fun channelActive(ctx: ChannelHandlerContext) {
                        println("[redis] ACTIVE ${meta(ctx)}")
                        super.channelActive(ctx)
                    }

                    override fun channelInactive(ctx: ChannelHandlerContext) {
                        println("[redis] INACTIVE ${meta(ctx)}")
                        super.channelInactive(ctx)
                    }

                    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
                        println("[redis] EXCEPTION ${meta(ctx)} cause=${cause::class.qualifiedName}: ${cause.message}")
                        cause.printStackTrace()
                        super.exceptionCaught(ctx, cause)
                    }

                    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
                        println("[redis] EVENT ${meta(ctx)} evt=${evt::class.qualifiedName} $evt")
                        super.userEventTriggered(ctx, evt)
                    }

                    override fun close(ctx: ChannelHandlerContext, promise: ChannelPromise) {
                        println("[redis] CLOSE called ${meta(ctx)}")
                        super.close(ctx, promise)
                    }
                })

                channel.closeFuture().addListener {
                    println("[redis][$id] CLOSE_FUTURE done success=${it.isSuccess} local=$local remote=$remote cause=${it.cause()?.message}")
                }
            }
        })

        return config
    }

    override fun <K : Any, V : Any> createSimpleCache(
        namespace: String,
        serializer: KSerializer<V>,
        ttl: Duration,
        keyToString: (K) -> String,
        redisApi: RedisApi
    ): SimpleRedisCache<K, V> {
        return SimpleRedisCacheImpl(namespace, serializer, keyToString, ttl, redisApi)
    }

    override fun <T : Any> createSimpleSetRedisCache(
        namespace: String,
        serializer: KSerializer<T>,
        ttl: Duration,
        idOf: (T) -> String,
        indexes: RedisSetIndexes<T>,
        redisApi: RedisApi
    ): SimpleSetRedisCache<T> {
        return SimpleSetRedisCacheImpl(namespace, serializer, idOf, indexes, ttl, redisApi)
    }

    override fun createEventBus(redisApi: RedisApi): RedisEventBus {
        return RedisEventBusImpl(redisApi)
    }

    override fun createRequestResponseBus(redisApi: RedisApi): RequestResponseBus {
        return RequestResponseBusImpl(redisApi)
    }

    override fun <E : Any> createSyncList(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncList<E> {
        return SyncListImpl(api, id, ttl, elementSerializer)
    }

    override fun <E : Any> createSyncSet(
        id: String,
        elementSerializer: KSerializer<E>,
        ttl: Duration,
        api: RedisApi
    ): SyncSet<E> {
        return SyncSetImpl(api, id, ttl, elementSerializer)
    }

    override fun <T : Any> createSyncValue(
        id: String,
        serializer: KSerializer<T>,
        defaultValue: T,
        ttl: Duration,
        api: RedisApi
    ): SyncValue<T> {
        return SyncValueImpl(api, id, serializer, defaultValue, ttl)
    }

    override fun <K : Any, V : Any> createSyncMap(
        id: String,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>,
        ttl: Duration,
        api: RedisApi
    ): SyncMap<K, V> {
        return SyncMapImpl(api, id, ttl, keySerializer, valueSerializer)
    }
}