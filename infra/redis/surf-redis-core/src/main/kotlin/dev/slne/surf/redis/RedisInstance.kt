package dev.slne.surf.redis

import dev.slne.surf.api.common.util.logger
import dev.slne.surf.redis.config.RedisConfig
import dev.slne.surf.redis.internal.SharedRedissonClientRegistry
import dev.slne.surf.redis.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Process-wide Redis networking: the Netty event loop, the Redisson executor, the Reactor
 * schedulers and the platform information the Redis modules need.
 *
 * The single implementation is found through [ServiceLoader][java.util.ServiceLoader] and created on
 * the first access of [instance], so a process that never uses Redis never starts a thread. Creating
 * it starts the event loop and the schedulers; [disable] stops them for good, and an instance cannot
 * be restarted afterwards. [disableIfStarted] is the shutdown hook for a host that may never have
 * used Redis.
 */
abstract class RedisInstance {
    val eventLoopGroup: MultiThreadIoEventLoopGroup
    val redissonExecutorService: ExecutorService

    init {
        val contextClassLoader = Thread.currentThread().contextClassLoader
        try {
            Thread.currentThread().contextClassLoader = this.javaClass.classLoader
            val nettyThreadFactory = Thread.ofPlatform()
                .name("redisson-netty-thread-", 0)
                .uncaughtExceptionHandler { thread, throwable ->
                    log.atSevere()
                        .withCause(throwable)
                        .log(
                            "Uncaught exception in Redisson Netty thread (%s): %s",
                            thread.name,
                            throwable
                        )
                }
                .factory()

            val redissonThreadFactory = Thread.ofVirtual()
                .name("redisson-virtual-thread-executor-", 0)
                .uncaughtExceptionHandler { thread, throwable ->
                    log.atSevere()
                        .withCause(throwable)
                        .log(
                            "Uncaught exception in Redisson virtual thread executor (%s): %s",
                            thread.name,
                            throwable
                        )
                }
                .factory()

            eventLoopGroup =
                MultiThreadIoEventLoopGroup(16, nettyThreadFactory, TransportInfo.instance.ioHandlerFactory)
            redissonExecutorService = Executors.newThreadPerTaskExecutor(redissonThreadFactory)
        } finally {
            Thread.currentThread().contextClassLoader = contextClassLoader
        }

        log.atInfo()
            .log(
                "Enabling Redis networking using %s transport with Redisson %s",
                TransportInfo.instance.transportString,
                RedisConstants.REDISSON_VERSION
            )
    }

    val streamPollScheduler: Scheduler = Schedulers.newBoundedElastic(
        8,
        Schedulers.DEFAULT_BOUNDED_ELASTIC_QUEUESIZE,
        "surf-redis-stream-poll",
        60,
        true
    )

    val ttlRefreshScheduler: Scheduler = Schedulers.newParallel("surf-redis-ttl-refresh", 2)

    abstract val dataPath: Path

    /**
     * Opens the Redis configuration file now instead of on the first connection.
     */
    fun load() {
        RedisConfig.init()
    }

    fun disable() {
        log.atInfo()
            .log("Disabling Redis networking")

        shutdownSharedRedissonClients()
        streamPollScheduler.dispose()
        ttlRefreshScheduler.dispose()
        eventLoopGroup.shutdownGracefully().syncUninterruptibly()

        redissonExecutorService.shutdown()
        if (!redissonExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
            redissonExecutorService.shutdownNow()
        }
    }

    private fun shutdownSharedRedissonClients() {
        val leaked = try {
            SharedRedissonClientRegistry.instance.shutdownAll()
        } catch (failure: Throwable) {
            log.atWarning()
                .withCause(failure)
                .log("Failed to shut down shared Redisson clients while disabling Redis networking")
            return
        }

        if (leaked.isEmpty()) return
        log.atWarning()
            .log(
                "%d shared Redisson client(s) were still in use while disabling Redis networking; " +
                        "their owners never called RedisApi.disconnect(): %s",
                leaked.size,
                leaked
            )
    }

    fun getResourceAsStream(name: String): InputStream? = javaClass.getResourceAsStream(name)

    abstract fun tryExtractPluginNameFromClass(clazz: Class<*>): String

    companion object {
        private val log = logger()
        private val holder = lazy { requiredService<RedisInstance>() }

        /**
         * The implementation, created on first access.
         */
        val instance: RedisInstance get() = holder.value

        fun get() = instance

        /**
         * Shuts the networking down if [instance] was ever accessed, and does nothing otherwise.
         */
        fun disableIfStarted() {
            if (holder.isInitialized()) holder.value.disable()
        }
    }
}