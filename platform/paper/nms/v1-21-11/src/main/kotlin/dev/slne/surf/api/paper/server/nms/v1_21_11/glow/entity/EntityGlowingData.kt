package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.V1_21_11SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11TeamData
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11NmsReflections
import glm_.and
import glm_.or
import net.minecraft.ChatFormatting

@NmsUseWithCaution
class EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: ChatFormatting?,
    var otherFlags: Byte,
    private val glowingBridge: V1_21_11SurfPaperNmsGlowingBridgeImpl,
) {

    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V1_21_11PacketOperationImpl.empty()

        val teamData = V1_21_11TeamData.getByColor(color)

        val operation = PacketOperation.start()

        if (teamData.markSeen(playerData.uuid)) {
            operation.add(glowingBridge.createTeam(teamData))
        }

        operation.add(glowingBridge.addEntityToTeam(teamData, teamId))

        return operation
    }

    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V1_21_11PacketOperationImpl.empty()

        val teamData = V1_21_11TeamData.getByColorOrNull(color)
            ?: return V1_21_11PacketOperationImpl.empty()

        val operation = PacketOperation.start()

        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(glowingBridge.removeEntityFromTeam(teamData, teamId))
        }

        return operation
    }

    fun sendGlowingFlag(enabled: Boolean, ignorePacket: Boolean = false): PacketOperation {
        val newFlags = if (enabled) {
            otherFlags or glowingFlag
        } else {
            otherFlags and glowingFlag.inv()
        }

        return glowingBridge.setEntityFlags(entityId, newFlags, ignorePacket)
    }

    fun computeFlags(): Byte {
        return (otherFlags and glowingFlag.inv()).or(if (color != null) glowingFlag else 0)
    }

    companion object {
        /**
         * The bit of the glowing flag in an entity's shared flags.
         */
        val glowingFlag = 1 shl V1_21_11NmsReflections.getEntityFlagGlowing()
    }
}
