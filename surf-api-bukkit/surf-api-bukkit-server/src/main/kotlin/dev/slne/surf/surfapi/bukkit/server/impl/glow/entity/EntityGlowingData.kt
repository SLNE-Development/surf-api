package dev.slne.surf.surfapi.bukkit.server.impl.glow.entity

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.server.impl.glow.SurfGlowingApiImpl
import dev.slne.surf.surfapi.bukkit.server.impl.glow.TeamData
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.glowingBridgeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
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
        val teamData = TeamData.Companion.getByColor(color)

        val operation = PacketOperation.start()
        if (teamData.markSeen(playerData.uuid)) {
            operation.add(glowingBridgeImpl.createTeam(teamData))
        }
        operation.add(glowingBridgeImpl.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return PacketOperationImpl.empty()
        val teamData = TeamData.Companion.getByColorOrNull(color) ?: return PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(glowingBridgeImpl.removeEntityFromTeam(teamData, teamId))
        }

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun sendGlowingFlag(enabled: Boolean, ignorePacket: Boolean = false): PacketOperation {
        val newFlags = if (enabled) {
            otherFlags or SurfGlowingApiImpl.Companion.glowingFlag
        } else {
            otherFlags and SurfGlowingApiImpl.Companion.glowingFlag.inv()
        }

        return glowingBridgeImpl.setEntityFlags(entityId, newFlags, ignorePacket)
    }

    fun computeFlags(): Byte {
        return (otherFlags and SurfGlowingApiImpl.Companion.glowingFlag.inv()).or(
            if (color != null) SurfGlowingApiImpl.Companion.glowingFlag else 0
        )
    }
}