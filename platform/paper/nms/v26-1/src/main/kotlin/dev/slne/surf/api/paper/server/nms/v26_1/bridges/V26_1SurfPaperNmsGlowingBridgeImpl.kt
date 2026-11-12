package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1TeamData
import dev.slne.surf.api.paper.server.nms.v26_1.packet.listener.V26_1GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1NmsReflections
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity

@NmsUseWithCaution
@Suppress("ClassName")
object V26_1SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    fun createTeam(data: V26_1TeamData): PacketOperation =
        V26_1PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }

    fun addEntityToTeam(data: V26_1TeamData, entry: String): PacketOperation =
        V26_1PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        }

    fun removeEntityFromTeam(data: V26_1TeamData, entry: String): PacketOperation =
        V26_1PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
            )
        }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation =
        V26_1PacketOperationImpl.simple {
            val dataAccessor = V26_1NmsReflections.getEntityDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            ClientboundSetEntityDataPacket(entityId, listOf(data)).also {
                if (ignorePacket) {
                    V26_1GlowingPacketListener.ignorePacket(it)
                }
            }
        }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = V26_1NmsReflections.getEntityDataFlagsSharedId()
        return entity.toNms().entityData.get(dataAccessor)
    }
}
