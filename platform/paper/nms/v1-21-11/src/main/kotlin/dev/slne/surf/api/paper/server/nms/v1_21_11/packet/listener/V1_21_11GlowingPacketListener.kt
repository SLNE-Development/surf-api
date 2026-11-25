package dev.slne.surf.api.paper.server.nms.v1_21_11.packet.listener

import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.common.NmsComponent
import dev.slne.surf.api.paper.packet.listener.listener.PacketListener
import dev.slne.surf.api.paper.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.V1_21_11SurfPaperNmsGlowingBridgeImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.V1_21_11SurfGlowingApiImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.glow.entity.EntityGlowingData
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11NmsReflections
import glm_.or
import net.minecraft.network.protocol.game.ClientboundBundlePacket
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData.DataValue
import org.bukkit.entity.Player

/**
 * Keeps the glowing flag set in the entity data a viewer receives for an entity glowing for them.
 */
@OptIn(NmsUseWithCaution::class)
@NmsComponent
class V1_21_11GlowingPacketListener(
    private val glowingApi: V1_21_11SurfGlowingApiImpl,
    private val glowingBridge: V1_21_11SurfPaperNmsGlowingBridgeImpl,
) : PacketListener {

    @ClientboundListener
    fun onBundlePacket(packet: ClientboundBundlePacket, player: Player): ClientboundBundlePacket {
        val bundles = packet.subPackets().toMutableList()
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
        player: Player,
    ): ClientboundSetEntityDataPacket {
        if (glowingBridge.consumeIgnoredPacket(packet)) {
            return packet
        }

        val playerData = glowingApi.getEntityPlayerData(player) ?: return packet
        val glowingData = playerData.entities[packet.id] ?: return packet

        val incoming = packet.packedItems
        var flagsFound = false
        var edited = false
        val newItems = ArrayList<DataValue<*>>(incoming.size + 1)
        val dataFlagsShared = V1_21_11NmsReflections.getEntityDataFlagsSharedId()
        val dataFlagsSharedId = dataFlagsShared.id
        val glowingFlag = EntityGlowingData.glowingFlag

        for (dataValue in incoming) {
            if (dataValue.id == dataFlagsSharedId) {
                flagsFound = true
                val current = dataValue.value as Byte
                glowingData.otherFlags = current
                val withGlow: Byte = current or glowingFlag

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
            val withGlow = glowingData.otherFlags or glowingFlag

            if (withGlow != 0.toByte()) {
                edited = true
                newItems.add(DataValue(dataFlagsSharedId, dataFlagsShared.serializer, withGlow))
            }
        }

        return if (edited) ClientboundSetEntityDataPacket(packet.id, newItems) else packet
    }
}
