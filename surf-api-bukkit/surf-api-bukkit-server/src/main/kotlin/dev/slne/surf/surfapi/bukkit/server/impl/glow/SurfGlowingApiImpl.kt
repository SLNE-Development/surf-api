package dev.slne.surf.surfapi.bukkit.server.impl.glow

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.extensions.server
import dev.slne.surf.surfapi.bukkit.api.glow.SurfGlowingApi
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.glowingBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.util.isChunkVisible
import dev.slne.surf.surfapi.bukkit.server.impl.glow.block.BlockGlowingData
import dev.slne.surf.surfapi.bukkit.server.impl.glow.block.BlockPlayerData
import dev.slne.surf.surfapi.bukkit.server.impl.glow.entity.EntityGlowingData
import dev.slne.surf.surfapi.bukkit.server.impl.glow.entity.EntityPlayerData
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
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
        val playerData = entityPlayerData.computeIfAbsent(uuid) { EntityPlayerData(uuid) }
        val glowingData = playerData.entities.get(targetId)
        val operation = PacketOperation.start()

        if (glowingData == null) {
            val newData = EntityGlowingData(
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
            safeExecutePacketOperation(operation, viewer)

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

        safeExecutePacketOperation(operation, viewer)
    }

    override fun makeGlowing(block: Block, viewer: Player, color: NamedTextColor) {
        makeGlowing(block.location, viewer, color)
    }

    override fun makeGlowing(location: Location, viewer: Player, color: NamedTextColor) {
        location.checkFinite()
        val location = location.toBlockLocation()
        val uuid = viewer.uniqueId
        val playerData = blockPlayerData.getOrPut(uuid) { BlockPlayerData(uuid) }
        val blockData = playerData.blocks.get(location)

        if (blockData == null) {
            val newData = BlockGlowingData(playerData, location, color)
            playerData.blocks[location] = newData

            if (viewer.isChunkVisible(location)) {
                safeExecutePacketOperation(newData.spawn(), viewer)
            }
        } else {
            blockData.color = color
            blockData.updateColor()
        }
    }

    override fun removeGlowing(target: Entity, viewer: Player) {
        removeGlowing(target.entityId, viewer)
    }

    fun removeGlowing(targetId: Int, viewer: UUID) {
        val player = server.getPlayer(viewer)

        if (player == null) {
            entityPlayerData[viewer]?.entities?.remove(targetId)
        } else {
            removeGlowing(targetId, player)
        }
    }

    override fun removeGlowing(targetId: Int, viewer: Player) {
        val playerData = entityPlayerData[viewer.uniqueId] ?: return
        val glowingData = playerData.entities.remove(targetId) ?: return
        val operation = glowingData.sendGlowingFlag(enabled = false) + glowingData.removeFromTeam()
        safeExecutePacketOperation(operation, viewer)
    }

    override fun removeGlowing(block: Block, viewer: Player) {
        removeGlowing(block.location, viewer)
    }

    override fun removeGlowing(location: Location, viewer: Player) {
        location.checkFinite()
        val location = location.toBlockLocation()
        val playerData = blockPlayerData[viewer.uniqueId] ?: return
        val blockData = playerData.blocks.remove(location) ?: return

        blockData.remove()
        if (playerData.blocks.isEmpty()) {
            blockPlayerData.remove(viewer.uniqueId)
        }
    }

    private fun teamIdFor(entity: Entity) = (entity as? Player)?.name ?: entity.uniqueId.toString()

    /**
     * Executes a packet operation safely on the main thread.
     * If already on the main thread, executes immediately.
     * Otherwise, schedules execution on the main thread.
     */
    private fun safeExecutePacketOperation(operation: PacketOperation, player: Player) {
        if (Bukkit.isPrimaryThread()) {
            operation.execute(player)
        } else {
            val plugin = JavaPlugin.getProvidingPlugin(SurfGlowingApi::class.java)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                operation.execute(player)
            })
        }
    }

    companion object {
        private val entityPlayerData = ConcurrentHashMap<UUID, EntityPlayerData>()
        private val blockPlayerData = ConcurrentHashMap<UUID, BlockPlayerData>()

        val glowingFlag = 1 shl Reflection.ENTITY_PROXY.getFlagGlowing()

        fun getEntityPlayerData(player: Player): EntityPlayerData? =
            entityPlayerData[player.uniqueId]

        fun getBlockPlayerData(player: Player): BlockPlayerData? =
            blockPlayerData[player.uniqueId]

        fun removeAllGlowingOnQuit(player: Player) {
            val uuid = player.uniqueId
            TeamData.removeFromAll(uuid)
            entityPlayerData.remove(uuid)?.entities?.clear()
            blockPlayerData.remove(uuid)?.blocks?.clear()
        }
    }
}

val glowingApiImpl get() = SurfGlowingApi.instance as SurfGlowingApiImpl