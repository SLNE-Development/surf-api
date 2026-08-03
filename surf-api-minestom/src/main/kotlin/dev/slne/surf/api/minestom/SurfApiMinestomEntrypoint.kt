package dev.slne.surf.api.minestom

import com.github.retrooper.packetevents.PacketEvents
import com.google.inject.Inject
import com.google.inject.Singleton
import dev.slne.minestom.lobby.api.plugin.MinestomPluginEntrypoint
import dev.slne.surf.api.core.extensions.packetEvents
import dev.slne.surf.api.core.server.packet.NoopPacketEvents
import dev.slne.surf.api.minestom.impl.SurfMinestomInstance

@Singleton
internal class SurfApiMinestomEntrypoint @Inject constructor() : MinestomPluginEntrypoint {
    override suspend fun start() {
        preparePacketEvents()
        SurfMinestomInstance.bootstrap()
        SurfMinestomInstance.onLoad()
        SurfMinestomInstance.onEnable()
    }

    override suspend fun stop() {
        SurfMinestomInstance.onDisable()
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