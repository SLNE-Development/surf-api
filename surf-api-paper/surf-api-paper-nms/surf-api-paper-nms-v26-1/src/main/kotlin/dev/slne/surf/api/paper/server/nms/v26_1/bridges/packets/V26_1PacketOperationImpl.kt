package dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets

import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import org.bukkit.entity.Player
import java.util.*

@Suppress("ClassName")
class V26_1PacketOperationImpl : PacketOperation {
    private val operations: ArrayList<Operation>

    private constructor(operation: Operation) {
        operations = arrayListOf(operation)
    }

    private constructor() {
        operations = ArrayList()
    }

    override fun execute(player: Player) {
        val connection = player.toNms().connection
        var packets: MutableList<Packet<in ClientGamePacketListener>> =
            ArrayList(operations.size.coerceAtLeast(4))
        for (operation in operations) {
            packets = operation.apply(player, packets)
        }

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

        operations.addAll(operation.operations)
        return this
    }

    override fun isEmpty(): Boolean {
        return operations.isEmpty()
    }

    fun interface Operation {
        fun apply(
            player: Player,
            packets: MutableList<Packet<in ClientGamePacketListener>>,
        ): MutableList<Packet<in ClientGamePacketListener>>
    }

    companion object {
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
