package dev.slne.surf.surfapi.bukkit.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.ServerboundCustomPayloadPacket.Payload
import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.BrandPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.DiscardedPayload

@NmsUseWithCaution
class ServerboundCustomPayloadPacketImpl(
    nmsPacket: ServerboundCustomPayloadPacket
) : NmsServerboundPacketImpl<ServerboundCustomPayloadPacket>(nmsPacket),
    dev.slne.surf.surfapi.bukkit.api.nms.listener.packets.serverbound.ServerboundCustomPayloadPacket {

    override var payload: Payload
        get() = toApiPayload(nmsPacket.payload);
        set(value) {
            nmsPacket = ServerboundCustomPayloadPacket(toNmsPayload(value))
        }

    companion object {
        private fun toApiPayload(nmsPayload: CustomPacketPayload): Payload = when (nmsPayload) {
            is BrandPayload -> Payload.Brand(nmsPayload.brand)
            is DiscardedPayload -> Payload.Discarded(
                id = PaperAdventure.asAdventure(nmsPayload.id),
                data = nmsPayload.data
            )

            else -> error("Unknown CustomPacketPayload type: ${nmsPayload.type()}")
        }

        private fun toNmsPayload(apiPayload: Payload): CustomPacketPayload = when (apiPayload) {
            is Payload.Brand -> BrandPayload(apiPayload.brand)
            is Payload.Discarded -> DiscardedPayload(
                PaperAdventure.asVanilla(apiPayload.id),
                apiPayload.data
            )
        }
    }
}