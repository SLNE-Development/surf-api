package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.NmsServerboundPacket
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.clientbound.ClientboundDisconnectPacketImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.CommandSuggestionPacketImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.RenameItemPacketImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound.SignUpdatePacketImpl
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.common.ClientCommonPacketListener
import net.minecraft.network.protocol.common.ClientboundDisconnectPacket
import net.minecraft.network.protocol.game.ServerGamePacketListener
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

        // Clientbound packets
        registerClientboundPacket(ClientboundDisconnectPacket::class) { ClientboundDisconnectPacketImpl(it) }
        // @formatter:on
    }

    private fun <Nms : Packet<ServerGamePacketListener>, Api : NmsServerboundPacket> registerServerboundPacket(
        nms: KClass<Nms>,
        factory: ServerboundPacketFactory<Nms, Api>,
    ) {
        SERVERBOUND_PACKETS.put(nms.java, factory)
    }

    fun <Nms : Packet<*>> createServerboundPacketOrNull(packet: Nms): NmsServerboundPacket? {
        val factory = SERVERBOUND_PACKETS[packet.javaClass] as? ServerboundPacketFactory<Nms, *>
        return factory?.create(packet)
    }

    private fun <Nms : Packet<Listener>, Api : NmsClientboundPacket, Listener : ClientCommonPacketListener> registerClientboundPacket(
        nms: KClass<Nms>,
        factory: ClientboundPacketFactory<Nms, Api>,
    ) {
        CLIENTBOUND_PACKETS.put(nms.java, factory)
    }

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
