package dev.slne.surf.api.paper.server.nms.v26_3.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.V26_3PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3TeamData
import dev.slne.surf.api.paper.server.nms.v26_3.packet.listener.V26_3GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_3.reflection.V26_3NmsReflections
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity

@NmsUseWithCaution
@Suppress("ClassName")
object V26_3SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    fun createTeam(data: V26_3TeamData): PacketOperation =
        V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }

    fun addEntityToTeam(data: V26_3TeamData, entry: String): PacketOperation =
        V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        }

    fun removeEntityFromTeam(data: V26_3TeamData, entry: String): PacketOperation =
        V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
            )
        }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation =
        V26_3PacketOperationImpl.simple {
            val dataAccessor = V26_3NmsReflections.getEntityDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            ClientboundSetEntityDataPacket(entityId, listOf(data)).also {
                if (ignorePacket) {
                    V26_3GlowingPacketListener.ignorePacket(it)
                }
            }
        }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = V26_3NmsReflections.getEntityDataFlagsSharedId()
        return entity.toNms().entityData.get(dataAccessor)
    }
}
