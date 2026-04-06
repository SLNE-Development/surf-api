package dev.slne.surf.api.paper.server.impl.glow.entity

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.server.impl.glow.SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.impl.glow.TeamData
import dev.slne.surf.api.paper.server.impl.nms.bridges.SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.impl.nms.bridges.packets.PacketOperationImpl
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
        val color = color ?: return PacketOperationImpl.empty()
        val teamData = TeamData.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(SurfPaperNmsGlowingBridgeImpl.INSTANCE.createTeam(teamData))
        }
        operation.add(SurfPaperNmsGlowingBridgeImpl.INSTANCE.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return PacketOperationImpl.empty()
        val teamData = TeamData.getByColorOrNull(color) ?: return PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(
                SurfPaperNmsGlowingBridgeImpl.INSTANCE.removeEntityFromTeam(
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
            otherFlags or SurfGlowingApiImpl.glowingFlag
        } else {
            otherFlags and SurfGlowingApiImpl.glowingFlag.inv()
        }

        return SurfPaperNmsGlowingBridgeImpl.INSTANCE.setEntityFlags(
            entityId,
            newFlags,
            ignorePacket
        )
    }

    fun computeFlags(): Byte {
        return (otherFlags and SurfGlowingApiImpl.glowingFlag.inv()).or(
            if (color != null) SurfGlowingApiImpl.glowingFlag else 0
        )
    }
}