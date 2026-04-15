package dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.api.core.util.mutableObjectListOf
import dev.slne.surf.api.core.util.toMutableObjectList
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11Reflection
import glm_.or
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

@OptIn(NmsUseWithCaution::class)
object V1_21_11GlowingPacketListener : PacketListener {

    val ignoreCache = Caffeine.newBuilder()
        .weakKeys()
        .expireAfterWrite(5.seconds)
        .build<Packet<*>, Unit>()

    fun ignorePacket(packet: Packet<*>) {
        ignoreCache.put(packet, Unit)
    }

    @ClientboundListener
    fun onBundlePacket(packet: ClientboundBundlePacket, player: Player): ClientboundBundlePacket {
        val bundles = packet.subPackets().toMutableObjectList()
        bundles.replaceAll { subPacket ->
            if (subPacket is ClientboundSetEntityDataPacket) {
                updatePacketIfNeeded(subPacket, player)
            } else {
                subPacket
            }
        }

        return ClientboundBundlePacket(bundles)
    }

    @ClientboundListener
    fun onSetEntityDataPacket(
        packet: ClientboundSetEntityDataPacket,
        player: Player,
    ): ClientboundSetEntityDataPacket {
        return updatePacketIfNeeded(packet, player)
    }

    private fun updatePacketIfNeeded(
        packet: ClientboundSetEntityDataPacket,
        player: Player
    ): ClientboundSetEntityDataPacket {
        // Ignore packets that we don't care about
        if (ignoreCache.asMap().remove(packet) != null) {
            return packet
        }

        val playerData = V1_21_11SurfGlowingApiImpl.getEntityPlayerData(player) ?: return packet
        val glowingData = playerData.entities[packet.id] ?: return packet
        val incoming = packet.packedItems
        var flagsFound = false
        var edited = false
        val newItems = mutableObjectListOf<DataValue<*>>(incoming.size + 1)
        val dataFlagsShared = V1_21_11Reflection.ENTITY_PROXY.getDataFlagsSharedId()
        val dataFlagsSharedId = dataFlagsShared.id

        for (dataValue in incoming) {
            if (dataValue.id == dataFlagsSharedId) {
                flagsFound = true
                val current = dataValue.value as Byte
                glowingData.otherFlags = current
                val withGlow: Byte = current or V1_21_11SurfGlowingApiImpl.glowingFlag

                if (withGlow != current) {
                    edited = true
                    newItems.add(DataValue(dataFlagsSharedId, dataFlagsShared.serializer, withGlow))
                } else {
                    newItems.add(dataValue)
                }
            } else {
                newItems.add(dataValue)
            }
        }

        if (!edited && !flagsFound) {
            val withGlow = glowingData.otherFlags or V1_21_11SurfGlowingApiImpl.glowingFlag
            if (withGlow != 0.toByte()) {
                edited = true
                newItems.add(DataValue(dataFlagsSharedId, dataFlagsShared.serializer, withGlow))
            }
        }

        return if (edited) ClientboundSetEntityDataPacket(packet.id, newItems) else packet
    }
}
