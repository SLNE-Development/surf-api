package dev.slne.surf.api.paper.nms.common

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.paper.nms.listener.packets.clientbound.NmsClientboundPacket
import dev.slne.surf.api.paper.nms.listener.packets.serverbound.NmsServerboundPacket

/**
 * Handles wrapping/unwrapping of NMS packets for the channel injector.
 *
 * This interface bridges the gap between raw NMS packet objects (passed as [Any])
 * and the API-level [NmsPacket] wrappers, allowing version-specific packet
 * handling without the channel injector needing to know about specific NMS types.
 */
@NmsUseWithCaution
interface NmsPacketBridgeHandler {
    /**
     * Wraps a raw NMS serverbound packet into an API [NmsServerboundPacket].
     *
     * @param nmsPacket the raw NMS packet object
     * @return the API wrapper, or `null` if no wrapper is available for this packet type
     */
    fun wrapServerboundPacket(nmsPacket: Any): NmsServerboundPacket?

    /**
     * Wraps a raw NMS clientbound packet into an API [NmsClientboundPacket].
     *
     * @param nmsPacket the raw NMS packet object
     * @return the API wrapper, or `null` if no wrapper is available for this packet type
     */
    fun wrapClientboundPacket(nmsPacket: Any): NmsClientboundPacket?

    /**
     * Extracts the underlying NMS packet from an API [NmsPacket] wrapper.
     *
     * @param apiPacket the API packet wrapper
     * @return the raw NMS packet object
     */
    fun unwrapPacket(apiPacket: NmsPacket): Any
}
