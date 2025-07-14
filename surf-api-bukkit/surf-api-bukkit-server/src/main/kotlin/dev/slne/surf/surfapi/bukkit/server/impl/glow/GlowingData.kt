package dev.slne.surf.surfapi.bukkit.server.impl.glow

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.glowingBridgeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
import glm_.and
import glm_.or
import net.minecraft.ChatFormatting

data class GlowingData(
    val playerData: PlayerData,
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
            operation.add(glowingBridgeImpl.createTeam(teamData))
        }
        operation.add(glowingBridgeImpl.addEntityToTeam(teamData, teamId))

        return operation
    }

    @OptIn(NmsUseWithCaution::class)
    fun removeFromTeam(): PacketOperation {
        val color = color ?: return PacketOperationImpl.empty()
        val teamData = TeamData.getByColorOrNull(color) ?: return PacketOperationImpl.empty()

        val operation = PacketOperation.start()
        if (teamData.removeSeen(playerData.uuid)) {
            operation.add(glowingBridgeImpl.removeEntityFromTeam(teamData, teamId))
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

        return glowingBridgeImpl.setEntityFlags(entityId, newFlags, ignorePacket)
    }
}