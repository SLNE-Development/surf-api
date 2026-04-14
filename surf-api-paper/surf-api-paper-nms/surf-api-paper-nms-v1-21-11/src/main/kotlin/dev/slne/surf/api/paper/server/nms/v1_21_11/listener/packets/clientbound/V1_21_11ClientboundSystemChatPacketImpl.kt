package dev.slne.surf.api.paper.server.nms.v1_21_11.listener.packets.clientbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.SystemChatPacket
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toBukkit
import net.kyori.adventure.text.Component
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundSystemChatPacket

@NmsUseWithCaution
class V1_21_11ClientboundSystemChatPacketImpl(nmsPacket: ClientboundSystemChatPacket) :
    V1_21_11NmsClientboundPacketImpl<ClientboundSystemChatPacket, ClientGamePacketListener>(nmsPacket),
    SystemChatPacket {
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
