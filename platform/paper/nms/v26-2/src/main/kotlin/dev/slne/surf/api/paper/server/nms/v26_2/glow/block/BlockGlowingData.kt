package dev.slne.surf.api.paper.server.nms.v26_2.glow.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.V26_2SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.V26_2PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import glm_.shl
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
import org.bukkit.craftbukkit.entity.CraftEntityType
import java.util.*

@OptIn(NmsUseWithCaution::class)
class BlockGlowingData(
    val playerData: BlockPlayerData,
    val location: Location,
    var color: NamedTextColor,
) {

    private val entityId: Int by lazy { SurfPaperNmsCommonBridge.nextEntityId() }
    private val uuid: UUID by lazy { UUID.randomUUID() }
    private var initialized = false

    fun spawn(): PacketOperation {
        initialize()

        val spawnOperation = V26_2PacketOperationImpl.simple {
            CraftEntityType.bukkitToMinecraft(org.bukkit.entity.EntityType.SHULKER)
            ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.x,
                location.y,
                location.z,
                location.pitch,
                location.yaw,
                EntityTypes.SHULKER,
                0,
                Vec3.ZERO,
                0.0
            )
        }
        val invisibleOperation = V26_2SurfPaperNmsGlowingBridgeImpl.setEntityFlags(entityId, invisibleFlag)

        return spawnOperation + invisibleOperation
    }


    fun updateColor() {
        val player = playerData.player ?: return
        V26_2SurfGlowingApiImpl.makeGlowing(
            entityId,
            uuid.toString(),
            player,
            color,
            invisibleFlag
        )
    }

    fun remove() {
        playerData.player?.let { SurfPaperNmsSpawnPackets.despawn(entityId).execute(it) }
        V26_2SurfGlowingApiImpl.removeGlowing(entityId, playerData.uuid)
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        updateColor()
    }

    companion object {
        val invisibleFlag = 1.toByte() shl V26_2NmsReflections.getEntityFlagInvisible()
    }
}
