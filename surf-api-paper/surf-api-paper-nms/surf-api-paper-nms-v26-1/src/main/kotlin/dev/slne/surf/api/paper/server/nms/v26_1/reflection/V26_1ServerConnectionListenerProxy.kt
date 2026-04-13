package dev.slne.surf.api.paper.server.nms.v26_1.reflection

import io.netty.channel.ChannelFuture
import net.minecraft.server.network.ServerConnectionListener
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(ServerConnectionListener::class)
interface V26_1ServerConnectionListenerProxy {
    @FieldGetter("channels")
    fun getChannels(instance: ServerConnectionListener): List<ChannelFuture>
}
