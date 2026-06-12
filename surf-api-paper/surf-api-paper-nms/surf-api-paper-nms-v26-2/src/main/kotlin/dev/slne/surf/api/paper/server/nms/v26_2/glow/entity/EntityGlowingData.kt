package dev.slne.surf.api.paper.server.nms.v26_2.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.V26_2PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2TeamData
import glm_.and
import glm_.or
import net.minecraft.world.scores.TeamColor

@NmsUseWithCaution
@Suppress("ClassName")
data class EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: TeamColor?,
    var otherFlags: Byte,
) {

    @OptIn(NmsUseWithCaution::class)
    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V26_2PacketOperationImpl.empty()
        val teamData = V26_2TeamData.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(V26_2SurfPaperNmsGlowingBridgeImpl.createTeam(teamData))
        }
        operation.add(V26_2SurfPaperNmsGlowingBridgeImpl.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V26_2PacketOperationImpl.empty()
        val teamData =
            V26_2TeamData.getByColorOrNull(color) ?: return V26_2PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(
                V26_2SurfPaperNmsGlowingBridgeImpl.removeEntityFromTeam(
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
            otherFlags or V26_2SurfGlowingApiImpl.glowingFlag
        } else {
            otherFlags and V26_2SurfGlowingApiImpl.glowingFlag.inv()
        }

        return V26_2SurfPaperNmsGlowingBridgeImpl.setEntityFlags(
            entityId,
            newFlags,
            ignorePacket
        )
    }

    fun computeFlags(): Byte {
        return (otherFlags and V26_2SurfGlowingApiImpl.glowingFlag.inv()).or(
            if (color != null) V26_2SurfGlowingApiImpl.glowingFlag else 0
        )
    }
}
