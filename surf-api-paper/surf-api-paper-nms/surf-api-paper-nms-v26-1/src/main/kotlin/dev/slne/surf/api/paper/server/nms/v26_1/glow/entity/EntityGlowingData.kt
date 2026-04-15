package dev.slne.surf.api.paper.server.nms.v26_1.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.V26_1SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1TeamData
import glm_.and
import glm_.or
import net.minecraft.ChatFormatting

@NmsUseWithCaution
@Suppress("ClassName")
data class EntityGlowingData(
    val playerData: EntityPlayerData,
    val entityId: Int,
    val teamId: String,
    var color: ChatFormatting?,
    var otherFlags: Byte,
) {

    @OptIn(NmsUseWithCaution::class)
    fun sendTeamColor(): PacketOperation {
        val color = color ?: return V26_1PacketOperationImpl.empty()
        val teamData = V26_1TeamData.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(V26_1SurfPaperNmsGlowingBridgeImpl.createTeam(teamData))
        }
        operation.add(V26_1SurfPaperNmsGlowingBridgeImpl.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return V26_1PacketOperationImpl.empty()
        val teamData = V26_1TeamData.getByColorOrNull(color) ?: return V26_1PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(
                V26_1SurfPaperNmsGlowingBridgeImpl.removeEntityFromTeam(
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
            otherFlags or V26_1SurfGlowingApiImpl.glowingFlag
        } else {
            otherFlags and V26_1SurfGlowingApiImpl.glowingFlag.inv()
        }

        return V26_1SurfPaperNmsGlowingBridgeImpl.setEntityFlags(
            entityId,
            newFlags,
            ignorePacket
        )
    }

    fun computeFlags(): Byte {
        return (otherFlags and V26_1SurfGlowingApiImpl.glowingFlag.inv()).or(
            if (color != null) V26_1SurfGlowingApiImpl.glowingFlag else 0
        )
    }
}
