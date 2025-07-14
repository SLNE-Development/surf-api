package dev.slne.surf.surfapi.bukkit.server.impl.glow

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.glow.SurfGlowingApi
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.glowingBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@AutoService(SurfGlowingApi::class)
class SurfGlowingApiImpl : SurfGlowingApi {

    @OptIn(NmsUseWithCaution::class)
    override fun makeGlowing(
        target: Entity,
        viewer: Player,
        color: NamedTextColor?,
    ) {
        makeGlowing(
            target.entityId,
            teamIdFor(target),
            viewer,
            color,
            glowingBridge.getCurrentFlags(target)
        )
    }

    override fun makeGlowing(
        targetId: Int,
        teamId: String,
        viewer: Player,
        color: NamedTextColor?,
        otherFlags: Byte,
    ) {
        val color = color?.let { PaperAdventure.asVanilla(it) }
        val uuid = viewer.uniqueId
        val playerData = playerTeamData.computeIfAbsent(uuid) { PlayerData(uuid) }
        val glowingData = playerData.entities.get(targetId)
        val operation = PacketOperation.start()

        if (glowingData == null) {
            val newData = GlowingData(
                playerData,
                targetId,
                teamId,
                color,
                otherFlags
            )
            playerData.entities.put(targetId, newData)

            operation.add(newData.sendGlowingFlag(enabled = true, ignorePacket = true))
            if (color != null) {
                operation.add(newData.sendTeamColor())
            }
            operation.execute(viewer)

            return
        }

        if (color == glowingData.color) return
        if (color == null) {
            operation.add(glowingData.removeFromTeam())
            glowingData.color = null
        } else {
            glowingData.color = color
            operation.add(glowingData.sendTeamColor())
        }

        operation.execute(viewer)
    }

    override fun removeGlowing(target: Entity, viewer: Player) {
        removeGlowing(target.entityId, viewer)
    }

    override fun removeGlowing(targetId: Int, viewer: Player) {
        val playerData = playerTeamData[viewer.uniqueId] ?: return
        val glowingData = playerData.entities.remove(targetId) ?: return
        val operation = glowingData.sendGlowingFlag(enabled = false) + glowingData.removeFromTeam()
        operation.execute(viewer)
    }

    private fun teamIdFor(entity: Entity) = (entity as? Player)?.name ?: entity.uniqueId.toString()

    companion object {
        private val playerTeamData = ConcurrentHashMap<UUID, PlayerData>()

        val glowingFlag = 1 shl Reflection.ENTITY_PROXY.getFlagGlowing()

        fun getPlayerData(player: Player): PlayerData? = playerTeamData[player.uniqueId]
        fun removeAllGlowingOnQuit(player: Player) {
            val uuid = player.uniqueId
            val playerData = playerTeamData.remove(uuid) ?: return
            TeamData.removeFromAll(uuid)
            playerData.entities.clear()
        }
    }
}