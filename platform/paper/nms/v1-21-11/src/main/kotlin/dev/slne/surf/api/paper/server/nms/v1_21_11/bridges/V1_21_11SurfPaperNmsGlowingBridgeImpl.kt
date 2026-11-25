package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsGlowingBridge
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11TeamData
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11NmsReflections
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
@NmsComponent
class V1_21_11SurfPaperNmsGlowingBridgeImpl : SurfPaperNmsGlowingBridge {
    private val ignoredPackets = Caffeine.newBuilder()
        .weakKeys()
        .expireAfterWrite(5.seconds)
        .build<Packet<*>, Unit>()

    fun createTeam(data: V1_21_11TeamData): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createAddOrModifyPacket(data.team, true)
        }
    }

    fun addEntityToTeam(data: V1_21_11TeamData, entry: String): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.ADD,
            )
        }
    }

    fun removeEntityFromTeam(data: V1_21_11TeamData, entry: String): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            ClientboundSetPlayerTeamPacket.createPlayerPacket(
                data.team,
                entry,
                ClientboundSetPlayerTeamPacket.Action.REMOVE,
            )
        }
    }

    fun setEntityFlags(entityId: Int, flags: Byte, ignorePacket: Boolean = false): PacketOperation {
        return V1_21_11PacketOperationImpl.simple {
            val dataAccessor = V1_21_11NmsReflections.getEntityDataFlagsSharedId()
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
        val dataAccessor = V1_21_11NmsReflections.getEntityDataFlagsSharedId()

        return entity.toNms().entityData.get(dataAccessor)
    }
}
