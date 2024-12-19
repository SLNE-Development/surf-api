package dev.slne.surf.surfapi.standalone

import com.github.retrooper.packetevents.PacketEvents
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
import dev.slne.surf.surfapi.core.api.extensions.packetEvents
import dev.slne.surf.surfapi.standalone.impl.SurfStandaloneInstance
import io.github.retrooper.packetevents.impl.netty.manager.player.PlayerManagerAbstract
import io.github.retrooper.packetevents.impl.netty.manager.protocol.ProtocolManagerAbstract
import io.github.retrooper.packetevents.impl.netty.manager.server.ServerManagerAbstract
import kotlinx.coroutines.runBlocking
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

object SurfApiStandaloneBootstrap {
    private val shutdown = AtomicBoolean(false)

    suspend fun bootstrap() {
        preparePacketEvents()

        SurfStandaloneInstance.bootstrap()
    }

    suspend fun enable() {
        SurfStandaloneInstance.onLoad()
        SurfStandaloneInstance.onEnable()

        Runtime.getRuntime()
            .addShutdownHook(thread(start = false) { runBlocking { shutdown() } })
    }

    suspend fun shutdown() {
        if (shutdown.getAndSet(true)) {
            return
        }

        SurfStandaloneInstance.onDisable()
        destroyPacketEvents()
    }

    private fun preparePacketEvents() {
        PacketEvents.setAPI(NoopPacketEvents())
        packetEvents.load()
        packetEvents.init()
    }

    private fun destroyPacketEvents() {
        packetEvents.terminate()
    }

    private class NoopPacketEvents : PacketEventsAPI<Any>() {
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
            throw UnsupportedOperationException("Not implemented")
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
            throw UnsupportedOperationException("Not implemented")
        }
    }

    private object NettyManagerHolder : NettyManager {
        override fun getChannelOperator(): ChannelOperator? {
            TODO("Not implemented")
        }

        override fun getByteBufOperator(): ByteBufOperator? {
            TODO("Not implemented")
        }

        override fun getByteBufAllocationOperator(): ByteBufAllocationOperator? {
            TODO("Not implemented")
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

        override fun isProxy(): Boolean {
            return false
        }
    }
}