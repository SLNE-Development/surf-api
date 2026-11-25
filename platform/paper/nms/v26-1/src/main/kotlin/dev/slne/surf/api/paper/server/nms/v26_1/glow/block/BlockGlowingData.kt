package dev.slne.surf.api.paper.server.nms.v26_1.glow.block

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsCommonBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.entity.SurfPaperNmsSpawnPackets
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.V26_1SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v26_1.bridges.packets.V26_1PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_1.glow.V26_1SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1NmsReflections
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
    private val glowingApi: V26_1SurfGlowingApiImpl,
    private val glowingBridge: V26_1SurfPaperNmsGlowingBridgeImpl,
) {

    private val entityId: Int by lazy { SurfPaperNmsCommonBridge.instance.nextEntityId() }
    private val uuid: UUID by lazy { UUID.randomUUID() }
    private var initialized = false

    fun spawn(): PacketOperation {
        initialize()

        val spawnOperation = V26_1PacketOperationImpl.simple {
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
                0.0,
            )
        }
        val invisibleOperation = glowingBridge.setEntityFlags(entityId, invisibleFlag)

        return spawnOperation + invisibleOperation
    }

    fun updateColor() {
        val player = playerData.player ?: return

        glowingApi.makeGlowing(
            entityId,
            uuid.toString(),
            player,
            color,
            invisibleFlag,
        )
    }

    fun remove() {
        playerData.player?.let { SurfPaperNmsSpawnPackets.instance.despawn(entityId).execute(it) }
        glowingApi.removeGlowing(entityId, playerData.uuid)
    }

    private fun initialize() {
        if (initialized) return

        initialized = true
        updateColor()
    }

    companion object {
        val invisibleFlag = 1.toByte() shl V26_1NmsReflections.getEntityFlagInvisible()
    }
}
