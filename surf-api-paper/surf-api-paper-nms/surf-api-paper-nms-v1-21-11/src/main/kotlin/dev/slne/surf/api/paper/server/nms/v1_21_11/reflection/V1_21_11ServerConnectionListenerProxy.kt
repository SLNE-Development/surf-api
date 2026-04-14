package dev.slne.surf.api.paper.server.nms.v1_21_11.reflection

import io.netty.channel.ChannelFuture
import net.minecraft.server.network.ServerConnectionListener
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(ServerConnectionListener::class)
interface V1_21_11ServerConnectionListenerProxy {
    @FieldGetter("channels")
    fun getChannels(instance: ServerConnectionListener): List<ChannelFuture>
}
