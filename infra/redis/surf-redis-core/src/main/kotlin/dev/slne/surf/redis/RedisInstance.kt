package dev.slne.surf.redis

import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.redis.config.RedisConfig
import io.netty.channel.MultiThreadIoEventLoopGroup
import reactor.core.scheduler.Scheduler
import reactor.core.scheduler.Schedulers
import java.io.InputStream
import java.nio.file.Path
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

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

    fun load() {
        log.atInfo()
            .log(
                "Enabling Redis networking using %s transport with Redisson %s",
                TransportInfo.instance.transportString,
                RedisConstants.REDISSON_VERSION
            )
        RedisConfig.init()
    }

    fun disable() {
        log.atInfo()
            .log("Disabling Redis networking")

        streamPollScheduler.dispose()
        ttlRefreshScheduler.dispose()
        eventLoopGroup.shutdownGracefully().syncUninterruptibly()

        redissonExecutorService.shutdown()
        if (!redissonExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
            redissonExecutorService.shutdownNow()
        }
    }

    fun getResourceAsStream(name: String): InputStream? = javaClass.getResourceAsStream(name)

    abstract fun tryExtractPluginNameFromClass(clazz: Class<*>): String

    companion object {
        private val log = logger()
        val instance = requiredService<RedisInstance>()
        fun get() = instance
    }
}