package dev.slne.surf.api.paper.api.nms.listener

import com.google.common.reflect.TypeToken
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.listener.packets.NmsPacket
import dev.slne.surf.api.shared.api.util.InternalSurfApi

/**
 * Receives the packets of type [Packet] that pass the player's connection.
 *
 * The packet type comes from the type argument of the implementing class, so implement the
 * interface in a class or object that names a concrete [Packet] type.
 */
@NmsUseWithCaution
interface NmsPacketListener<Packet : NmsPacket>

/**
 * Resolves the packet type this listener is registered for from the type argument of its class.
 *
 * Returns the bound [NmsPacket] when the class leaves the type argument open.
 */
@InternalSurfApi
@NmsUseWithCaution
fun NmsPacketListener<*>.resolvePacketClass(): Class<*> {
    val packetType = NmsPacketListener::class.java.typeParameters.single()

    return TypeToken.of(javaClass)
        .resolveType(packetType)
        .rawType
}
