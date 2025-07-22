package dev.slne.surf.surfapi.bukkit.server.reflection

import io.netty.channel.ChannelFuture
import net.minecraft.server.network.ServerConnectionListener
import xyz.jpenilla.reflectionremapper.proxy.annotation.FieldGetter
import xyz.jpenilla.reflectionremapper.proxy.annotation.Proxies

@Proxies(ServerConnectionListener::class)
interface ServerConnectionListenerProxy {
    @FieldGetter("channels")
    fun getChannels(instance: ServerConnectionListener): List<ChannelFuture>
}