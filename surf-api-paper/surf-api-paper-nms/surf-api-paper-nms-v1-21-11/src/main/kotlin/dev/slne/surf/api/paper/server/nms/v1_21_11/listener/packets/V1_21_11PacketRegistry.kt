package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.clientbound.V1_21_11ClientboundDisconnectPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.clientbound.V1_21_11ClientboundSystemChatPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound.V1_21_11CommandSuggestionPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound.V1_21_11RenameItemPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound.V1_21_11ServerboundCustomPayloadPacketImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.serverbound.V1_21_11SignUpdatePacketImpl
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.common.ServerCommonPacketListener
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket
import net.minecraft.network.protocol.game.ServerboundCommandSuggestionPacket
import net.minecraft.network.protocol.game.ServerboundRenameItemPacket
import net.minecraft.network.protocol.game.ServerboundSignUpdatePacket
import kotlin.reflect.KClass

@OptIn(NmsUseWithCaution::class)
object V1_21_11PacketRegistry {
    private typealias PacketMap<F> = Object2ObjectOpenHashMap<Class<out Packet<*>>, F>

    private val SERVERBOUND_PACKETS = PacketMap<ServerboundPacketFactory<*, *>>()
    private val CLIENTBOUND_PACKETS = PacketMap<ClientboundPacketFactory<*, *>>()

    init {
        // @formatter:off
        // Serverbound packets
        registerServerboundPacket(ServerboundSignUpdatePacket::class) { V1_21_11SignUpdatePacketImpl(it) }
        registerServerboundPacket(ServerboundRenameItemPacket::class) { V1_21_11RenameItemPacketImpl(it) }
        registerServerboundPacket(ServerboundCommandSuggestionPacket::class) { V1_21_11CommandSuggestionPacketImpl(it) }
        registerServerboundPacket(ServerboundCustomPayloadPacket::class) { V1_21_11ServerboundCustomPayloadPacketImpl(it) }

        // Clientbound packets
        registerClientboundPacket(ClientboundDisconnectPacket::class) { V1_21_11ClientboundDisconnectPacketImpl(it) }
        registerClientboundPacket(ClientboundSystemChatPacket::class) { V1_21_11ClientboundSystemChatPacketImpl(it) }
        // @formatter:on
    }

    private fun <Nms : Packet<out ServerCommonPacketListener>, Api : NmsServerboundPacket> registerServerboundPacket(
        nms: KClass<Nms>,
        factory: ServerboundPacketFactory<Nms, Api>,
    ) {
        SERVERBOUND_PACKETS[nms.java] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <Nms : Packet<*>> createServerboundPacketOrNull(packet: Nms): NmsServerboundPacket? {
        val factory = SERVERBOUND_PACKETS[packet.javaClass] as? ServerboundPacketFactory<Nms, *>
        return factory?.create(packet)
    }

    private fun <Nms : Packet<Listener>, Api : NmsClientboundPacket, Listener : ClientCommonPacketListener> registerClientboundPacket(
        nms: KClass<Nms>,
        factory: ClientboundPacketFactory<Nms, Api>,
    ) {
        CLIENTBOUND_PACKETS[nms.java] = factory
    }

    @Suppress("UNCHECKED_CAST")
    fun <Nms : Packet<*>> createClientboundPacketOrNull(packet: Nms): NmsClientboundPacket? {
        val factory = CLIENTBOUND_PACKETS[packet.javaClass] as? ClientboundPacketFactory<Nms, *>
        return factory?.create(packet)
    }

    private fun interface ServerboundPacketFactory<Nms : Packet<*>, Api : NmsServerboundPacket> {
        fun create(packet: Nms): Api
    }

    private fun interface ClientboundPacketFactory<Nms : Packet<*>, Api : NmsClientboundPacket> {
        fun create(packet: Nms): Api
    }
}
