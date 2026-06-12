package dev.slne.surf.api.paper.server.nms.v26_2.listener.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.clientbound.V26_1ClientboundDisconnectPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.clientbound.V26_1ClientboundSystemChatPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.serverbound.V26_1CommandSuggestionPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.serverbound.V26_1RenameItemPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.serverbound.V26_1ServerboundCustomPayloadPacketImpl
import dev.slne.surf.api.paper.server.nms.v26_1.listener.packets.serverbound.V26_1SignUpdatePacketImpl
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
@Suppress("ClassName")
object V26_2PacketRegistry {
    private typealias PacketMap<F> = Object2ObjectOpenHashMap<Class<out Packet<*>>, F>

    private val SERVERBOUND_PACKETS = PacketMap<ServerboundPacketFactory<*, *>>()
    private val CLIENTBOUND_PACKETS = PacketMap<ClientboundPacketFactory<*, *>>()

    init {
        // @formatter:off
        // Serverbound packets
        registerServerboundPacket(ServerboundSignUpdatePacket::class) { V26_1SignUpdatePacketImpl(it) }
        registerServerboundPacket(ServerboundRenameItemPacket::class) { V26_1RenameItemPacketImpl(it) }
        registerServerboundPacket(ServerboundCommandSuggestionPacket::class) { V26_1CommandSuggestionPacketImpl(it) }
        registerServerboundPacket(ServerboundCustomPayloadPacket::class) { V26_1ServerboundCustomPayloadPacketImpl(it) }

        // Clientbound packets
        registerClientboundPacket(ClientboundDisconnectPacket::class) { V26_1ClientboundDisconnectPacketImpl(it) }
        registerClientboundPacket(ClientboundSystemChatPacket::class) { V26_1ClientboundSystemChatPacketImpl(it) }
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
