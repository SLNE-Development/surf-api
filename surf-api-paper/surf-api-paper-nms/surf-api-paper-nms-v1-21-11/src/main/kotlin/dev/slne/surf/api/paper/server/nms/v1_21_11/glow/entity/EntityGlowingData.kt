package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.V1_21_11SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11TeamData
import glm_.and
import glm_.or
import net.minecraft.ChatFormatting

data class EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: ChatFormatting?,
    var otherFlags: Byte,
) {

    @OptIn(NmsUseWithCaution::class)
    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V1_21_11PacketOperationImpl.empty()
        val teamData = V1_21_11TeamData.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(V1_21_11SurfPaperNmsGlowingBridgeImpl.INSTANCE.createTeam(teamData))
        }
        operation.add(V1_21_11SurfPaperNmsGlowingBridgeImpl.INSTANCE.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V1_21_11PacketOperationImpl.empty()
        val teamData = V1_21_11TeamData.getByColorOrNull(color) ?: return V1_21_11PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(
                V1_21_11SurfPaperNmsGlowingBridgeImpl.INSTANCE.removeEntityFromTeam(
                    teamData,
                    teamId
                )
            )
        }

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun sendGlowingFlag(enabled: Boolean, ignorePacket: Boolean = false): PacketOperation {
        val newFlags = if (enabled) {
            otherFlags or V1_21_11SurfGlowingApiImpl.glowingFlag
        } else {
            otherFlags and V1_21_11SurfGlowingApiImpl.glowingFlag.inv()
        }

        return V1_21_11SurfPaperNmsGlowingBridgeImpl.INSTANCE.setEntityFlags(
            entityId,
            newFlags,
            ignorePacket
        )
    }

    fun computeFlags(): Byte {
        return (otherFlags and V1_21_11SurfGlowingApiImpl.glowingFlag.inv()).or(
            if (color != null) V1_21_11SurfGlowingApiImpl.glowingFlag else 0
        )
    }
}
