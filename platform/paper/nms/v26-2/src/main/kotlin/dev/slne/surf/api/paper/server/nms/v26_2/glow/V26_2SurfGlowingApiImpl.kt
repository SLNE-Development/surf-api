package dev.slne.surf.api.paper.server.nms.v26_2.glow

import com.github.shynixn.mccoroutine.folia.entityDispatcher
import com.github.shynixn.mccoroutine.folia.launch
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.glow.SurfGlowingApi
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.common.NmsProvider
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.block.BlockGlowingData
import dev.slne.surf.api.paper.server.nms.v26_2.glow.block.BlockPlayerData
import dev.slne.surf.api.paper.server.nms.v26_2.glow.entity.EntityPlayerData
import dev.slne.surf.api.paper.server.nms.v26_2.glow.entity.V26_2EntityGlowingData
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import dev.slne.surf.api.paper.util.isChunkVisible
import io.papermc.paper.adventure.PaperAdventure
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.block.Block
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import java.util.*
import java.util.concurrent.ConcurrentHashMap

@NmsUseWithCaution
@Suppress("ClassName")
object V26_2SurfGlowingApiImpl : SurfGlowingApi {
    private val entityPlayerData = ConcurrentHashMap<UUID, EntityPlayerData>()
    private val blockPlayerData = ConcurrentHashMap<UUID, BlockPlayerData>()

    val glowingFlag = 1 shl V26_2NmsReflections.getEntityFlagGlowing()

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
            V26_2SurfPaperNmsGlowingBridgeImpl.getCurrentFlags(target)
        )
    }

    override fun makeGlowing(
        targetId: Int,
        teamId: String,
        viewer: Player,
        color: NamedTextColor?,
        otherFlags: Byte,
    ) {
        val nmsColor = color?.let { PaperAdventure.asVanilla(it) }
        val uuid = viewer.uniqueId
        val playerData = entityPlayerData.computeIfAbsent(uuid) { EntityPlayerData(uuid) }
        val glowingData = playerData.entities[targetId]
        val operation = PacketOperation.start()

        if (glowingData == null) {
            val newData = V26_2EntityGlowingData(
                playerData,
                targetId,
                teamId,
                nmsColor,
                otherFlags
            )
            playerData.entities[targetId] = newData

            operation.add(newData.sendGlowingFlag(enabled = true, ignorePacket = true))
            if (nmsColor != null) {
                operation.add(newData.sendTeamColor())
            }
            operation.execute(viewer)
            return
        }

        if (nmsColor == glowingData.color) return
        if (nmsColor == null) {
            operation.add(glowingData.removeFromTeam())
            glowingData.color = null
        } else {
            glowingData.color = nmsColor
            operation.add(glowingData.sendTeamColor())
        }

        operation.execute(viewer)
    }

    override fun makeGlowing(block: Block, viewer: Player, color: NamedTextColor) {
        makeGlowing(block.location, viewer, color)
    }

    override fun makeGlowing(location: Location, viewer: Player, color: NamedTextColor) {
        location.checkFinite()
        val blockLocation = location.toBlockLocation()
        val uuid = viewer.uniqueId
        val playerData = blockPlayerData.getOrPut(uuid) { BlockPlayerData(uuid) }
        val blockData = playerData.blocks[blockLocation]

        if (blockData == null) {
            val newData = BlockGlowingData(playerData, blockLocation, color)
            playerData.blocks[blockLocation] = newData

            val plugin = NmsProvider.current.plugin
            plugin.launch(plugin.entityDispatcher(viewer)) {
                if (viewer.isChunkVisible(blockLocation)) {
                    newData.spawn().execute(viewer)
                }
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
        operation.execute(viewer)
    }

    override fun removeGlowing(block: Block, viewer: Player) {
        removeGlowing(block.location, viewer)
    }

    override fun removeGlowing(location: Location, viewer: Player) {
        location.checkFinite()
        val blockLocation = location.toBlockLocation()
        val playerData = blockPlayerData[viewer.uniqueId] ?: return
        val blockData = playerData.blocks.remove(blockLocation) ?: return

        blockData.remove()
        if (playerData.blocks.isEmpty()) {
            blockPlayerData.remove(viewer.uniqueId)
        }
    }

    private fun teamIdFor(entity: Entity) = (entity as? Player)?.name ?: entity.uniqueId.toString()


    fun getEntityPlayerData(player: Player): EntityPlayerData? =
        entityPlayerData[player.uniqueId]

    fun getBlockPlayerData(player: Player): BlockPlayerData? =
        blockPlayerData[player.uniqueId]

    fun removeAllGlowingOnQuit(player: Player) {
        val uuid = player.uniqueId
        V26_2TeamData.removeFromAll(uuid)
        entityPlayerData.remove(uuid)?.entities?.clear()
        blockPlayerData.remove(uuid)?.blocks?.clear()
    }
}
