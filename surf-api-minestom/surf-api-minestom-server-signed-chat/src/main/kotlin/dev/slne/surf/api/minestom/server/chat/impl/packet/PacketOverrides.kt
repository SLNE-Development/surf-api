package dev.slne.surf.api.minestom.server.chat.impl.packet

import net.minestom.server.MinecraftServer
import net.minestom.server.network.NetworkBuffer
import net.minestom.server.network.packet.PacketVanilla
import net.minestom.server.network.packet.PacketWriting
import net.minestom.server.network.packet.server.FramedPacket
import net.minestom.server.network.packet.server.ServerPacket

@Suppress("UnstableApiUsage")
internal object PacketOverrides {

    private val ids = object : ClassValue<Int?>() {
        override fun computeValue(type: Class<*>): Int? {
            return PacketVanilla.SERVER_PACKET_PARSER.play().packetInfo(type).id()
        }
    }

    fun idOf(packet: Class<out ServerPacket.Play>): Int {
        return ids.get(packet) ?: -1
    }

    fun frame(
        packet: OverridingPacket,
        compressionThreshold: Int = MinecraftServer.getCompressionThreshold(),
        initialBufferSize: Long = INITIAL_BUFFER_SIZE
    ): FramedPacket {
        val buffer =
            NetworkBuffer.resizableBuffer(initialBufferSize, MinecraftServer.getRegistries())

        PacketWriting.writeFramedPacket(
            buffer,
            BODY_TYPE,
            idOf(packet.overrides),
            packet,
            compressionThreshold
        )

        return FramedPacket(packet, buffer.copy(0, buffer.writeIndex()))
    }

    private const val INITIAL_BUFFER_SIZE = 512L

    private val BODY_TYPE = object : NetworkBuffer.Type<OverridingPacket> {
        override fun write(buffer: NetworkBuffer, value: OverridingPacket) = value.write(buffer)

        override fun read(buffer: NetworkBuffer): OverridingPacket =
            throw UnsupportedOperationException("Overriding packets are outbound only")
    }
}

@Suppress("UnstableApiUsage")
internal fun OverridingPacket.framed(): FramedPacket = PacketOverrides.frame(this)
