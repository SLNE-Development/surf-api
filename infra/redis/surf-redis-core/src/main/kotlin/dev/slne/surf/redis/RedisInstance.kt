package dev.slne.surf.redis

import dev.slne.surf.surfapi.core.api.util.logger
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.netty.channel.MultiThreadIoEventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.nio.NioIoHandler
import java.nio.file.Path

abstract class RedisInstance {
    private val factory = if (Epoll.isAvailable()) {
        EpollIoHandler.newFactory()
    } else {
        NioIoHandler.newFactory()
    }

    val eventLoopGroup = MultiThreadIoEventLoopGroup(factory)

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
    }

    companion object {
        private val log = logger()
        val instance = requiredService<RedisInstance>()
    }
}