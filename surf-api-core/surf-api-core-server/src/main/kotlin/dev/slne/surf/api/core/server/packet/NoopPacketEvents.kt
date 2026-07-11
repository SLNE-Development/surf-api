package dev.slne.surf.api.core.server.packet

import com.github.retrooper.packetevents.PacketEventsAPI
import com.github.retrooper.packetevents.injector.ChannelInjector
import com.github.retrooper.packetevents.manager.player.PlayerManager
import com.github.retrooper.packetevents.manager.protocol.ProtocolManager
import com.github.retrooper.packetevents.manager.server.ServerManager
import com.github.retrooper.packetevents.manager.server.ServerVersion
import com.github.retrooper.packetevents.netty.NettyManager
import com.github.retrooper.packetevents.netty.buffer.ByteBufAllocationOperator
import com.github.retrooper.packetevents.netty.buffer.ByteBufOperator
import com.github.retrooper.packetevents.netty.channel.ChannelOperator
import com.github.retrooper.packetevents.protocol.ProtocolVersion
import com.github.retrooper.packetevents.protocol.player.User
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract
import io.github.retrooper.packetevents.impl.netty.manager.protocol.ProtocolManagerAbstract
import io.github.retrooper.packetevents.impl.netty.manager.server.ServerManagerAbstract

class NoopPacketEvents : PacketEventsAPI<Any>() {
    override fun load() {
    }

    override fun isLoaded(): Boolean {
        return false
    }

    override fun init() {
    }

    override fun isInitialized(): Boolean {
        return false
    }

    override fun terminate() {
    }

    override fun isTerminated(): Boolean {
        return false
    }

    override fun getPlugin(): Any? {
        return null
    }

    override fun getServerManager(): ServerManager {
        return ServerManagerHolder
    }

    override fun getProtocolManager(): ProtocolManager {
        return ProtocolManagerHolder
    }

    override fun getPlayerManager(): PlayerManager {
        return PlayerManagerHolder
    }

    override fun getNettyManager(): NettyManager {
        return NettyManagerHolder
    }

    override fun getInjector(): ChannelInjector {
        return ChannelInjectorHolder
    }
}

private object ServerManagerHolder : ServerManagerAbstract() {
    override fun getVersion(): ServerVersion? {
        return ServerVersion.getLatest()
    }
}

private object ProtocolManagerHolder : ProtocolManagerAbstract() {
    override fun getPlatformVersion(): ProtocolVersion {
        return ProtocolVersion.UNKNOWN
    }
}

private object PlayerManagerHolder : PlayerManagerAbstract() {
    override fun getPing(player: Any): Int {
        return -1
    }

    override fun getChannel(player: Any): Any? {
        return null
    }
}

private object NettyManagerHolder : NettyManager {
    override fun getChannelOperator(): ChannelOperator? {
        return null
    }

    override fun getByteBufOperator(): ByteBufOperator? {
        return null
    }

    override fun getByteBufAllocationOperator(): ByteBufAllocationOperator? {
        return null
    }
}

private object ChannelInjectorHolder : ChannelInjector {
    override fun inject() {
    }

    override fun uninject() {
    }

    override fun updateUser(channel: Any?, user: User?) {
    }

    override fun setPlayer(channel: Any?, player: Any?) {
    }

    override fun isPlayerSet(p0: Any?): Boolean {
        return false
    }

    override fun isProxy(): Boolean {
        return false
    }
}
