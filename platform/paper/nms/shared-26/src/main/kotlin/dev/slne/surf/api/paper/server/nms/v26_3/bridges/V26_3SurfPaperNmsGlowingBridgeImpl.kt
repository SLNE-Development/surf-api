package dev.slne.surf.api.paper.server.nms.v26_3.bridges

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.server.nms.v26_3.bridges.packets.V26_3PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_3.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_3.glow.V26_3TeamData
import dev.slne.surf.api.paper.server.nms.v26_3.reflection.V26_3NmsReflections
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Entity
import kotlin.time.Duration.Companion.seconds

/**
 * Builds the team and entity-flag packets of the glowing API.
 *
 * Flag packets created with `ignorePacket` are remembered for five seconds, so the glowing packet
 * listener passes them through unchanged instead of adding the glowing flag again.
 */
@NmsUseWithCaution
@Suppress("ClassName")
@NmsComponent
class V26_3SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    private val ignoredPackets = Caffeine.newBuilder()
        .weakKeys()
        .expireAfterWrite(5.seconds)
        .build<Packet<*>, Unit>()

    fun createTeam(data: V26_3TeamData): PacketOperation {
        return V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }
    }

    fun addEntityToTeam(data: V26_3TeamData, entry: String): PacketOperation {
        return V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD,
            )
        }
    }

    fun removeEntityFromTeam(data: V26_3TeamData, entry: String): PacketOperation {
        return V26_3PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE,
            )
        }
    }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation {
        return V26_3PacketOperationImpl.simple {
            val dataAccessor = V26_3NmsReflections.getEntityDataFlagsSharedId()
            val data = DataValue(dataAccessor.id(), dataAccessor.serializer, flags)
            val packet = ClientboundSetEntityDataPacket(entityId, listOf(data))
            if (ignorePacket) {
                ignoredPackets.put(packet, Unit)
            }

            packet
        }
    }

    /**
     * Returns whether [packet] was created by [setEntityFlags] with `ignorePacket` and forgets it.
     */
    fun consumeIgnoredPacket(packet: Packet<*>): Boolean {
        return ignoredPackets.asMap().remove(packet) != null
    }

    override fun getCurrentFlags(entity: Entity): Byte {
        val dataAccessor = V26_3NmsReflections.getEntityDataFlagsSharedId()

        return entity.toNms().entityData.get(dataAccessor)
    }
}
