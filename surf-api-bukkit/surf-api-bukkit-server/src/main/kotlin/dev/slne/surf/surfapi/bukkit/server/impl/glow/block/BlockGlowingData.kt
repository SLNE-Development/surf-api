package dev.slne.surf.surfapi.bukkit.server.impl.glow.block

import dev.slne.surf.surfapi.bukkit.api.glow.glowingApi
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.SurfBukkitNmsCommonBridge
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.nmsSpawnPackets
import dev.slne.surf.surfapi.bukkit.server.impl.glow.glowingApiImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.glowingBridgeImpl
import dev.slne.surf.surfapi.bukkit.server.impl.nms.bridges.packets.PacketOperationImpl
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
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

    private val entityId: Int by lazy { SurfBukkitNmsCommonBridge.nextEntityId }
    private val uuid: UUID by lazy { UUID.randomUUID() }
    private var initialized = false

    fun spawn(): PacketOperation {
        initialize()

        val spawnOperation = PacketOperationImpl.simple {
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
        val invisibleOperation = glowingBridgeImpl.setEntityFlags(entityId, invisibleFlag)


        return spawnOperation + invisibleOperation
    }


    fun updateColor() {
        val player = playerData.player ?: return
        glowingApi.makeGlowing(entityId, uuid.toString(), player, color, invisibleFlag)
    }

    fun remove() {
        playerData.player?.let { nmsSpawnPackets.despawn(entityId).execute(it) }
        glowingApiImpl.removeGlowing(entityId, playerData.uuid)
    }

    private fun initialize() {
        if (initialized) return
        initialized = true
        updateColor()
    }

    companion object {
        val invisibleFlag = 1.toByte() shl Reflection.ENTITY_PROXY.getFlagInvisible()
    }
}