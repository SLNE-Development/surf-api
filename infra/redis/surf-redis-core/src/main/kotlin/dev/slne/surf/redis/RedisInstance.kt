package dev.slne.surf.redis

import dev.slne.surf.redis.config.RedisConfig
import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.nio.NioIoHandler
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
        val ioHandlerFactory = if (Epoll.isAvailable()) EpollIoHandler.newFactory() else NioIoHandler.newFactory()
        val nettyThreadFactory = Thread.ofPlatform()
            .name("redisson-netty-thread-", 0)
            .uncaughtExceptionHandler { thread, throwable ->
                log.atSevere()
                    .withCause(throwable)
                    .log("Uncaught exception in Redisson Netty thread (%s): %s", thread.name, throwable)
            }
            .factory()

        val redissonThreadFactory = Thread.ofVirtual()
            .name("redisson-virtual-thread-executor-", 0)
            .uncaughtExceptionHandler { thread, throwable ->
                log.atSevere()
                    .withCause(throwable)
                    .log("Uncaught exception in Redisson virtual thread executor (%s): %s", thread.name, throwable)
            }
            .factory()

        eventLoopGroup = MultiThreadIoEventLoopGroup(4, nettyThreadFactory, ioHandlerFactory)
        redissonExecutorService = Executors.newThreadPerTaskExecutor(redissonThreadFactory)
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
        if (Epoll.isAvailable()) {
            log.atInfo()
                .log("Using Epoll for Redis networking")
        } else {
            log.atInfo()
                .log("Using NIO for Redis networking")
        }
        RedisConfig.init()
    }

    fun disable() {
        log.atInfo()
            .log("Disabling Redis networking")

        eventLoopGroup.shutdownGracefully().syncUninterruptibly()
        streamPollScheduler.dispose()
        ttlRefreshScheduler.dispose()

        redissonExecutorService.shutdown()
        if (!redissonExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
            redissonExecutorService.shutdownNow()
        }
    }

    fun getResourceAsStream(name: String): InputStream? = javaClass.getResourceAsStream(name)

    companion object {
        private val log = logger()
        val instance = requiredService<RedisInstance>()
        fun get() = instance
    }
}