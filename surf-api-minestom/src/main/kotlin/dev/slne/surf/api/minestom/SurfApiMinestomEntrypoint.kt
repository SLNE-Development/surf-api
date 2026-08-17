package dev.slne.surf.api.minestom

import com.github.retrooper.packetevents.PacketEvents
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.minestom.impl.SurfMinestomInstance

@Singleton
internal class SurfApiMinestomEntrypoint @Inject constructor(
    private val instance: SurfMinestomInstance
) : MinestomPluginEntrypoint {
    override suspend fun start() {
        preparePacketEvents()
        instance.bootstrap()
        instance.onLoad()
        instance.onEnable()
    }

    override suspend fun stop() {
        instance.onDisable()
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