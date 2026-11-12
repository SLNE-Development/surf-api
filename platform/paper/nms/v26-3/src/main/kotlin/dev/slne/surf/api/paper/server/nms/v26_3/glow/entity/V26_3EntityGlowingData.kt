package dev.slne.surf.api.paper.server.nms.v26_3.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.V26_3SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.V26_3PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3TeamData
import glm_.and
import glm_.or
import net.minecraft.world.scores.TeamColor

@NmsUseWithCaution
@Suppress("ClassName")
data class V26_3EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: TeamColor?,
    var otherFlags: Byte,
) {

    @OptIn(NmsUseWithCaution::class)
    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V26_3PacketOperationImpl.empty()
        val teamData = V26_3TeamData.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(V26_3SurfPaperNmsGlowingBridgeImpl.createTeam(teamData))
        }
        operation.add(V26_3SurfPaperNmsGlowingBridgeImpl.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V26_3PacketOperationImpl.empty()
        val teamData = V26_3TeamData.getByColorOrNull(color) ?: return V26_3PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(
                V26_3SurfPaperNmsGlowingBridgeImpl.removeEntityFromTeam(
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
            otherFlags or V26_3SurfGlowingApiImpl.glowingFlag
        } else {
            otherFlags and V26_3SurfGlowingApiImpl.glowingFlag.inv()
        }

        return V26_3SurfPaperNmsGlowingBridgeImpl.setEntityFlags(
            entityId,
            newFlags,
            ignorePacket
        )
    }

    fun computeFlags(): Byte {
        return (otherFlags and V26_3SurfGlowingApiImpl.glowingFlag.inv()).or(
            if (color != null) V26_3SurfGlowingApiImpl.glowingFlag else 0
        )
    }
}
