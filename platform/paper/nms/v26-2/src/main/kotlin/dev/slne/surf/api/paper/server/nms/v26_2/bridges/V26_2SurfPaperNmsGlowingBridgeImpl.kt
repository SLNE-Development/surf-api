package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.V26_2PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2TeamData
import dev.slne.surf.api.paper.server.nms.v26_2.packet.listener.V26_2GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity

@NmsUseWithCaution
@Suppress("ClassName")
object V26_2SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    fun createTeam(data: V26_2TeamData): PacketOperation =
        V26_2PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }

    fun addEntityToTeam(data: V26_2TeamData, entry: String): PacketOperation =
        V26_2PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        }

    fun removeEntityFromTeam(data: V26_2TeamData, entry: String): PacketOperation =
        V26_2PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
            )
        }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation =
        V26_2PacketOperationImpl.simple {
            val dataAccessor = V26_2NmsReflections.getEntityDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            ClientboundSetEntityDataPacket(entityId, listOf(data)).also {
                if (ignorePacket) {
                    V26_2GlowingPacketListener.ignorePacket(it)
                }
            }
        }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = V26_2NmsReflections.getEntityDataFlagsSharedId()
        return entity.toNms().entityData.get(dataAccessor)
    }
}
