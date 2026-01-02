package dev.slne.surf.redis

import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.nio.NioIoHandler
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

    abstract val dataPath: Path

    fun load() {
        if (Epoll.isAvailable()) {
            log.atInfo()
                .log("Using Epoll for Redis networking")
        } else {
            log.atInfo()
                .log("Using NIO for Redis networking")
        }
    }

    fun disable() {
        log.atInfo()
            .log("Disabling Redis networking")
        eventLoopGroup.shutdownGracefully().syncUninterruptibly()

        redissonExecutorService.shutdown()
        if (!redissonExecutorService.awaitTermination(5, TimeUnit.SECONDS)) {
            redissonExecutorService.shutdownNow()
        }
    }

    companion object {
        private val log = logger()
        val instance = requiredService<RedisInstance>()
    }
}