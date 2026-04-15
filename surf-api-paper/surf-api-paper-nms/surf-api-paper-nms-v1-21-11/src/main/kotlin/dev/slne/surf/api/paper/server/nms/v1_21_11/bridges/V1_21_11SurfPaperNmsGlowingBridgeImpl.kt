package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11TeamData
import dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener.V1_21_11GlowingPacketListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11Reflection
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity

@NmsUseWithCaution
object V1_21_11SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    fun createTeam(data: V1_21_11TeamData): PacketOperation =
        V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }

    fun addEntityToTeam(data: V1_21_11TeamData, entry: String): PacketOperation =
        V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        }

    fun removeEntityFromTeam(data: V1_21_11TeamData, entry: String): PacketOperation =
        V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
            )
        }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation =
        V1_21_11PacketOperationImpl.simple {
            val dataAccessor = V1_21_11Reflection.ENTITY_PROXY.getDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            ClientboundSetEntityDataPacket(entityId, listOf(data)).also {
                if (ignorePacket) {
                    V1_21_11GlowingPacketListener.ignorePacket(it)
                }
            }
        }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = V1_21_11Reflection.ENTITY_PROXY.getDataFlagsSharedId()
        return entity.toNms().entityData.get(dataAccessor)
    }
}
