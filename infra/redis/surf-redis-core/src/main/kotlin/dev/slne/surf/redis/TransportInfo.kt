package dev.slne.surf.redis

import io.netty.channel.IoHandlerFactory
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollIoHandler
import io.netty.channel.kqueue.KQueue
import io.netty.channel.kqueue.KQueueIoHandler
import io.netty.channel.nio.NioIoHandler
import io.netty.channel.uring.IoUring
import io.netty.channel.uring.IoUringIoHandler
import org.redisson.config.TransportMode

@ConsistentCopyVisibility
data class TransportInfo private constructor(
    val ioHandlerFactory: IoHandlerFactory,
    val redissonTransportMode: TransportMode,
    val transportString: String
) {

    companion object {
        val instance: TransportInfo by lazy { detect() }

        private fun detect(): TransportInfo = when {
            IoUring.isAvailable() -> TransportInfo(
                ioHandlerFactory = IoUringIoHandler.newFactory(),
                redissonTransportMode = TransportMode.IO_URING,
                transportString = "IoUring"
            )

            Epoll.isAvailable() -> TransportInfo(
                ioHandlerFactory = EpollIoHandler.newFactory(),
                redissonTransportMode = TransportMode.EPOLL,
                transportString = "Epoll"
            )

            KQueue.isAvailable() -> TransportInfo(
                ioHandlerFactory = KQueueIoHandler.newFactory(),
                redissonTransportMode = TransportMode.KQUEUE,
                transportString = "KQueue"
            )

            else -> TransportInfo(
                ioHandlerFactory = NioIoHandler.newFactory(),
                redissonTransportMode = TransportMode.NIO,
                transportString = "NIO"
            )
        }
    }
}
