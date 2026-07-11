package dev.slne.surf.api.paper.server.nms.v26_2.packet.listener

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.toMutableObjectList
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.api.paper.server.nms.v26_2.glow.V26_2SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v26_2.glow.entity.EntityPlayerData
import dev.slne.surf.api.paper.server.nms.v26_2.reflection.V26_2NmsReflections
import glm_.or
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

@OptIn(NmsUseWithCaution::class)
@Suppress("ClassName")
object V26_2GlowingPacketListener : PacketListener {

    val ignoreCache = Caffeine.newBuilder()
        .weakKeys()
        .expireAfterWrite(5.seconds)
        .build<Packet<*>, Unit>()

    fun ignorePacket(packet: Packet<*>) {
        ignoreCache.put(packet, Unit)
    }

    @ClientboundListener
    fun onBundlePacket(packet: ClientboundBundlePacket, player: Player): ClientboundBundlePacket {
        val playerData = V26_2SurfGlowingApiImpl.getEntityPlayerData(player) ?: return packet
        val bundles = packet.subPackets().toMutableObjectList()
        var changed = false
        bundles.replaceAll { subPacket ->
            if (subPacket is ClientboundSetEntityDataPacket) {
                updatePacketIfNeeded(subPacket, playerData).also {
                    changed = changed || it !== subPacket
                }
            } else {
                subPacket
            }
        }

        return if (changed) ClientboundBundlePacket(bundles) else packet
    }

    @ClientboundListener
    fun onSetEntityDataPacket(
        packet: ClientboundSetEntityDataPacket,
        player: Player,
    ): ClientboundSetEntityDataPacket {
        val playerData = V26_2SurfGlowingApiImpl.getEntityPlayerData(player) ?: return packet
        return updatePacketIfNeeded(packet, playerData)
    }

    private fun updatePacketIfNeeded(
        packet: ClientboundSetEntityDataPacket,
        playerData: EntityPlayerData,
    ): ClientboundSetEntityDataPacket {
        // Ignore packets that we don't care about
        if (ignoreCache.asMap().remove(packet) != null) {
            return packet
        }

        val glowingData = playerData.entities[packet.id] ?: return packet
        val incoming = packet.packedItems
        val dataFlagsShared = V26_2NmsReflections.getEntityDataFlagsSharedId()
        val dataFlagsSharedId = dataFlagsShared.id

        for (index in incoming.indices) {
            val dataValue = incoming[index]
            if (dataValue.id == dataFlagsSharedId) {
                val current = dataValue.value as Byte
                glowingData.otherFlags = current
                val withGlow: Byte = current or V26_2SurfGlowingApiImpl.glowingFlag
                if (withGlow == current) return packet

                val newItems = mutableObjectListOf<DataValue<*>>(incoming.size)
                newItems.addAll(incoming)
                newItems[index] = DataValue(dataFlagsSharedId, dataFlagsShared.serializer, withGlow)
                return ClientboundSetEntityDataPacket(packet.id, newItems)
            }
        }

        val newItems = mutableObjectListOf<DataValue<*>>(incoming.size + 1)
        newItems.addAll(incoming)
        val withGlow = glowingData.otherFlags or V26_2SurfGlowingApiImpl.glowingFlag
        newItems.add(DataValue(dataFlagsSharedId, dataFlagsShared.serializer, withGlow))
        return ClientboundSetEntityDataPacket(packet.id, newItems)
    }
}
