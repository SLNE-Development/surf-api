package dev.slne.surf.surfapi.bukkit.server.impl.glow

import com.github.benmanes.caffeine.cache.Caffeine
import com.sksamuel.aedile.core.expireAfterWrite
import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.PacketListener
import dev.slne.surf.surfapi.bukkit.api.packet.listener.listener.annotation.ClientboundListener
import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import dev.slne.surf.surfapi.core.api.util.mutableObjectListOf
import glm_.or
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket
import net.minecraft.network.syncher.SynchedEntityData
import org.bukkit.entity.Player
import kotlin.time.Duration.Companion.seconds

@OptIn(NmsUseWithCaution::class)
object GlowingPacketListener: PacketListener {

    val ignoreCache = Caffeine.newBuilder()
        .weakKeys()
        .expireAfterWrite(5.seconds)
        .build<Packet<*>, Unit>()

    fun ignorePacket(packet: Packet<*>) {
        ignoreCache.put(packet, Unit)
    }

    @ClientboundListener
    fun onSetEntityDataPacket(
        packet: ClientboundSetEntityDataPacket,
        player: Player,
    ): ClientboundSetEntityDataPacket {
        // Ignore packets that we don't care about
        if (ignoreCache.asMap().remove(packet) != null) {
            return packet
        }

        val playerData = SurfGlowingApiImpl.getPlayerData(player) ?: return packet
        val glowingData = playerData.entities.get(packet.id) ?: return packet
        val incoming = packet.packedItems
        var flagsFound = false
        val newItems = mutableObjectListOf<SynchedEntityData.DataValue<*>>(incoming.size + 1)
        val dataFlagsShared = Reflection.ENTITY_PROXY.getDataFlagsSharedId()
        val dataFlagsSharedId = dataFlagsShared.id

        for (dataValue in incoming) {
            if (dataValue.id == dataFlagsSharedId) {
                flagsFound = true
                val current = dataValue.value as Byte
                glowingData.otherFlags = current
                val withGlow: Byte = current or SurfGlowingApiImpl.glowingFlag
                newItems.add(
                    SynchedEntityData.DataValue(
                        dataFlagsSharedId,
                        dataFlagsShared.serializer,
                        withGlow
                    )
                )
            } else {
                newItems.add(dataValue)
            }
        }

        if (!flagsFound) {
            // Add our own flags value (assume not glowing yet)
            val newVal = glowingData.otherFlags or SurfGlowingApiImpl.glowingFlag
            newItems.add(SynchedEntityData.DataValue(dataFlagsSharedId, dataFlagsShared.serializer, newVal))
        }

        return ClientboundSetEntityDataPacket(packet.id, newItems)
    }
}