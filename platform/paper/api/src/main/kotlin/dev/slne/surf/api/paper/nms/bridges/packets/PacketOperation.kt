package dev.slne.surf.api.paper.nms.bridges.packets

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import org.bukkit.entity.Player
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

interface PacketOperation {
    fun execute(player: Player)
    fun add(operation: PacketOperation): PacketOperation
    fun isEmpty(): Boolean

    operator fun plus(other: PacketOperation) = add(other)

    companion object {
        @OptIn(NmsUseWithCaution::class) // not really nms here
        fun start(): PacketOperation = SurfPaperNmsPacketBridges.INSTANCE.createEmptyPacketOperation()
    }
}

/**
 * Builds a packet operation by configuring an empty one.
 *
 * ```kotlin
 * PacketOperation { add(SurfPaperNmsSpawnPackets.INSTANCE.despawn(ids)) }.execute(player)
 * ```
 */
@OptIn(ExperimentalContracts::class)
inline fun PacketOperation(block: PacketOperation.() -> Unit): PacketOperation {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return PacketOperation.start().apply(block)
}
