package dev.slne.surf.api.paper.server.impl.nms.listener.packets.serverbound

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import io.papermc.paper.adventure.PaperAdventure
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket
import net.minecraft.network.protocol.common.custom.BrandPayload
import net.minecraft.network.protocol.common.custom.CustomPacketPayload
import net.minecraft.network.protocol.common.custom.DiscardedPayload
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.ServerboundCustomPayloadPacket.Payload as ApiPayload

@NmsUseWithCaution
class ServerboundCustomPayloadPacketImpl(
    nmsPacket: ServerboundCustomPayloadPacket
) : NmsServerboundPacketImpl<ServerboundCustomPayloadPacket>(nmsPacket),
    dev.slne.surf.api.paper.nms.listener.packets.serverbound.ServerboundCustomPayloadPacket {

    override var payload: ApiPayload
        get() = toApiPayload(nmsPacket.payload);
        set(value) {
            nmsPacket = ServerboundCustomPayloadPacket(toNmsPayload(value))
        }

    companion object {
        private fun toApiPayload(nmsPayload: CustomPacketPayload): ApiPayload = when (nmsPayload) {
            is BrandPayload -> ApiPayload.Brand(nmsPayload.brand)
            is DiscardedPayload -> ApiPayload.Discarded(
                id = PaperAdventure.asAdventure(nmsPayload.id),
                data = nmsPayload.data
            )

            else -> error("Unknown CustomPacketPayload type: ${nmsPayload.type()}")
        }

        private fun toNmsPayload(apiPayload: ApiPayload): CustomPacketPayload = when (apiPayload) {
            is ApiPayload.Brand -> BrandPayload(apiPayload.brand)
            is ApiPayload.Discarded -> DiscardedPayload(
                PaperAdventure.asVanilla(apiPayload.id),
                apiPayload.data
            )
        }
    }
}