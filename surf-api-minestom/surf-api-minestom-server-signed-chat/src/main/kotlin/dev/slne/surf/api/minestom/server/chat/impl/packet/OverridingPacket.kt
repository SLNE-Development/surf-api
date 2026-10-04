package dev.slne.surf.api.minestom.server.chat.impl.packet

import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.packet.server.ServerPacket

internal interface OverridingPacket : ServerPacket.Play {

    val overrides: Class<out ServerPacket.Play>
    fun write(buffer: NetworkBuffer)
}
