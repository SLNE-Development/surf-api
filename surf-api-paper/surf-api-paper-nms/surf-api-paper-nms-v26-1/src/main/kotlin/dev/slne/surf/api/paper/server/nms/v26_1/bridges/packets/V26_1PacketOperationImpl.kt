package dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets

import com.google.common.flogger.FluentLogger
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.entity.Player
import java.util.*

@Suppress("ClassName")
class V26_1PacketOperationImpl : PacketOperation {
    private var operation: Operation

    private constructor(operation: Operation) {
        this.operation = operation
    }

    private constructor() {
        this.operation = Operation.empty()
    }

    override fun execute(player: Player) {
        val connection = player.toNms().connection
        val packets = operation.apply(
            player,
            LinkedList<Packet<in ClientGamePacketListener>>()
        )

        if (packets.isEmpty()) {
            return
        }

        if (packets.size == 1) {
            connection.send(packets.first())
            return
        }

        connection.send(ClientboundBundlePacket(packets))
    }

    override fun add(operation: PacketOperation): V26_1PacketOperationImpl {
        require(operation is V26_1PacketOperationImpl) { "operation must be an instance of V26_1PacketOperationImpl" }

        this.operation = this.operation.andThen(operation.operation)
        return this
    }

    override fun isEmpty(): Boolean {
        val operation = operation
        return operation is EmptyOperation && operation.empty
    }

    fun interface Operation {
        fun apply(
            player: Player,
            packets: LinkedList<Packet<in ClientGamePacketListener>>,
        ): LinkedList<Packet<in ClientGamePacketListener>>

        fun andThen(after: Operation): Operation {
            return Operation { player, packets ->
                after.apply(player, apply(player, packets))
            }
        }

        companion object {
            fun empty() = EmptyOperation()
        }
    }

    class EmptyOperation : Operation {
        var empty: Boolean = true
            private set

        override fun apply(
            player: Player,
            packets: LinkedList<Packet<in ClientGamePacketListener>>,
        ): LinkedList<Packet<in ClientGamePacketListener>> {
            return packets
        }

        override fun andThen(after: Operation): Operation {
            empty = false
            return super.andThen(after)
        }
    }

    companion object {
        private val logger: FluentLogger = FluentLogger.forEnclosingClass()

        @JvmStatic
        fun empty(): V26_1PacketOperationImpl {
            return V26_1PacketOperationImpl()
        }

        @JvmStatic
        fun complex(operation: Operation): V26_1PacketOperationImpl {
            return V26_1PacketOperationImpl(operation)
        }

        fun simple(packetSupplier: (Player) -> Packet<in ClientGamePacketListener>): V26_1PacketOperationImpl {
            return V26_1PacketOperationImpl { player, packets ->
                packets.add(packetSupplier(player))
                packets
            }
        }

        fun task(task: (Player) -> Unit): V26_1PacketOperationImpl {
            return V26_1PacketOperationImpl { player, packets ->
                task(player)
                packets
            }
        }
    }
}
