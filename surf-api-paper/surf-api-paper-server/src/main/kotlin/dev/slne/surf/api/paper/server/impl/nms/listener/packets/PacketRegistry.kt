package dev.slne.surf.api.paper.server.impl.nms.listener.packets

import dev.slne.surf.api.core.util.mutableObject2ObjectMapOf
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.clientbound.ClientboundDisconnectPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.clientbound.ClientboundSystemChatPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound.CommandSuggestionPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound.RenameItemPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound.ServerboundCustomPayloadPacketImpl
import dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound.SignUpdatePacketImpl
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
object PacketRegistry {
    private val SERVERBOUND_PACKETS =
        mutableObject2ObjectMapOf<Class<out Packet<*>>, ServerboundPacketFactory<*, *>>()
    private val CLIENTBOUND_PACKETS =
        mutableObject2ObjectMapOf<Class<out Packet<*>>, ClientboundPacketFactory<*, *>>()

    init {
        // @formatter:off
        // Serverbound packets
        registerServerboundPacket(ServerboundSignUpdatePacket::class) { SignUpdatePacketImpl(it) }
        registerServerboundPacket(ServerboundRenameItemPacket::class) { RenameItemPacketImpl(it) }
        registerServerboundPacket(ServerboundCommandSuggestionPacket::class) { CommandSuggestionPacketImpl(it) }
        registerServerboundPacket(ServerboundCustomPayloadPacket::class) { ServerboundCustomPayloadPacketImpl(it) }

        // Clientbound packets
        registerClientboundPacket(ClientboundDisconnectPacket::class) { ClientboundDisconnectPacketImpl(it) }
        registerClientboundPacket(ClientboundSystemChatPacket::class) { ClientboundSystemChatPacketImpl(it) }
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
