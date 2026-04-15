package dev.slne.surf.api.paper.server.nms.v1_21_11.glow.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.V1_21_11SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11Reflection
import glm_.shl
import net.kyori.adventure.text.format.NamedTextColor
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket
import net.minecraft.world.entity.EntityType
import net.minecraft.world.phys.Vec3
import org.bukkit.Location
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

        val spawnOperation = V1_21_11PacketOperationImpl.simple {
            ClientboundAddEntityPacket(
                entityId,
                uuid,
                location.x,
                location.y,
                location.z,
                location.pitch,
                location.yaw,
                EntityType.SHULKER,
                0,
                Vec3.ZERO,
                0.0
            )
        }
        val invisibleOperation = V1_21_11SurfPaperNmsGlowingBridgeImpl.setEntityFlags(entityId, invisibleFlag)

        return spawnOperation + invisibleOperation
    }


    fun updateColor() {
        val player = playerData.player ?: return
        V1_21_11SurfGlowingApiImpl.makeGlowing(
            entityId,
            uuid.toString(),
            player,
            color,
            invisibleFlag
        )
    }

    fun remove() {
        playerData.player?.let { SurfPaperNmsSpawnPackets.despawn(entityId).execute(it) }
        V1_21_11SurfGlowingApiImpl.removeGlowing(entityId, playerData.uuid)
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        updateColor()
    }

    companion object {
        val invisibleFlag = 1.toByte() shl V1_21_11Reflection.ENTITY_PROXY.getFlagInvisible()
    }
}
