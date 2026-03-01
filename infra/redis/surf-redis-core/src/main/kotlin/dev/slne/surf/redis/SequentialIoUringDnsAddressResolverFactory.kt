package dev.slne.surf.redis

import io.netty.channel.socket.DatagramChannel
import io.netty.channel.socket.SocketChannel
import io.netty.channel.uring.IoUringDatagramChannel
import io.netty.channel.uring.IoUringSocketChannel
import io.netty.resolver.AddressResolverGroup
import io.netty.resolver.dns.DnsServerAddressStreamProvider
import org.redisson.connection.SequentialDnsAddressResolverFactory
import java.net.InetSocketAddress

class SequentialIoUringDnsAddressResolverFactory(private val useIoUring: Boolean) :
    SequentialDnsAddressResolverFactory() {
    override fun create(
        channelType: Class<out DatagramChannel>,
        socketChannelType: Class<out SocketChannel>,
        nameServerProvider: DnsServerAddressStreamProvider
    ): AddressResolverGroup<InetSocketAddress?>? {
        return if (useIoUring) {
            super.create(IoUringDatagramChannel::class.java, IoUringSocketChannel::class.java, nameServerProvider)
        } else {
            super.create(channelType, socketChannelType, nameServerProvider)
        }
    }
}