package dev.slne.surf.api.paper.server.impl.nms.bridges

import com.google.auto.service.AutoService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.impl.glow.GlowingPacketListener
import dev.slne.surf.api.paper.server.impl.glow.TeamData
import dev.slne.surf.api.paper.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.toNms
import dev.slne.surf.api.paper.server.reflection.Reflection
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity

@NmsUseWithCaution
@AutoService(SurfPaperNmsGlowingBridge::class)
class SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    fun createTeam(data: TeamData): PacketOperation =
        PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }

    fun addEntityToTeam(data: TeamData, entry: String): PacketOperation =
        PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD
            )
        }

    fun removeEntityFromTeam(data: TeamData, entry: String): PacketOperation =
        PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE
            )
        }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation =
        PacketOperationImpl.simple {
            val dataAccessor = Reflection.ENTITY_PROXY.getDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            ClientboundSetEntityDataPacket(entityId, listOf(data)).also {
                if (ignorePacket) {
                    GlowingPacketListener.ignorePacket(it)
                }
            }
        }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = Reflection.ENTITY_PROXY.getDataFlagsSharedId()
        return entity.toNms().entityData.get(dataAccessor)
    }

    companion object {
        val INSTANCE get() = SurfPaperNmsGlowingBridge.INSTANCE as SurfPaperNmsGlowingBridgeImpl
    }
}