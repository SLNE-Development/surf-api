package dev.slne.surf.surfapi.hytale.server

import com.github.retrooper.packetevents.PacketEvents
import dev.slne.surf.surfapi.core.api.extensions.packetEvents
import dev.slne.surf.surfapi.core.server.CoreInstance
import dev.slne.surf.surfapi.core.server.packet.NoopPacketEvents

object HytaleInstance : CoreInstance() {
    override suspend fun bootstrap() {
        super.bootstrap()

        PacketEvents.setAPI(NoopPacketEvents())
        packetEvents.load()
        packetEvents.init()
    }

    override suspend fun onEnable() {
        super.onEnable()
    }

    override suspend fun onDisable() {
        super.onDisable()
        packetEvents.terminate()
    }
}