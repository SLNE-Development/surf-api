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
        fun start(): PacketOperation = SurfPaperNmsPacketBridges.createEmptyPacketOperation()
    }
}

@OptIn(ExperimentalContracts::class)
@JvmName("PacketOperationInline")
inline fun PacketOperation(block: PacketOperation.() -> Unit): PacketOperation {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return PacketOperation.start().apply(block)
}

@Deprecated(message = "Use the inline version instead", level = DeprecationLevel.HIDDEN)
@OptIn(ExperimentalContracts::class)
fun PacketOperation(block: PacketOperation.() -> Unit): PacketOperation {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }

    return PacketOperation.start().apply(block)
}