package dev.slne.surf.api.standalone

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.standalone.impl.SurfStandaloneInstance
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
}