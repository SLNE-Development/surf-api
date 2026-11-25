package dev.slne.surf.api.paper.server.nms.v26_1.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.V26_1SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1TeamData
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1NmsReflections
import glm_.and
import glm_.or
import net.minecraft.ChatFormatting

@NmsUseWithCaution
@Suppress("ClassName")
class V26_1EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: ChatFormatting?,
    var otherFlags: Byte,
    private val glowingBridge: V26_1SurfPaperNmsGlowingBridgeImpl,
) {

    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V26_1PacketOperationImpl.empty()

        val teamData = V26_1TeamData.getByColor(color)

        val operation = PacketOperation.start()

        if (teamData.markSeen(playerData.uuid)) {
            operation.add(glowingBridge.createTeam(teamData))
        }

        operation.add(glowingBridge.addEntityToTeam(teamData, teamId))

        return operation
    }

    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V26_1PacketOperationImpl.empty()

        val teamData = V26_1TeamData.getByColorOrNull(color)
            ?: return V26_1PacketOperationImpl.empty()

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
        val glowingFlag = 1 shl V26_1NmsReflections.getEntityFlagGlowing()
    }
}
