package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.clientbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.clientbound.SystemChatPacket
import dev.slne.surf.surfapi.bukkit.server.nms.toBukkit
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

@NmsUseWithCaution
class ClientboundSystemChatPacketImpl(nmsPacket: ClientboundSystemChatPacket) :
    NmsClientboundPacketImpl<ClientboundSystemChatPacket>(nmsPacket), SystemChatPacket {
    override var content: Component
        get() = nmsPacket.content().toBukkit()
        set(value) {
            nmsPacket = ClientboundSystemChatPacket(value, nmsPacket.overlay)
        }

    override var overlay: Boolean
        get() = nmsPacket.overlay()
        set(value) {
            nmsPacket = ClientboundSystemChatPacket(nmsPacket.content(), value)
        }
}