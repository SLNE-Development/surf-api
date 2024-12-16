package dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity

import dev.slne.surf.surfapi.bukkit.api.nms.NmsUseWithCaution
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.PacketOperation
import dev.slne.surf.surfapi.bukkit.api.nms.bridges.packets.entity.SpawnPacketsSettingsBuilder.*
import dev.slne.surf.surfapi.core.api.util.requiredService
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.FinePosition
import it.unimi.dsi.fastutil.ints.IntList

@NmsUseWithCaution
interface SurfBukkitNmsSpawnPacketsNew {

    fun despawn(entityIds: IntList): PacketOperation
    fun despawn(vararg entityId: Int): PacketOperation

    fun spawnItemDisplay(
        entityId: Int,
        position: FinePosition,
        settings: ItemDisplaySettings
    ): PacketOperation

    fun spawnItemDisplay(
        entityId: Int,
        position: FinePosition,
        settings: ItemDisplaySettings.ItemDisplaySettingsBuilder<*, *>.() -> Unit
    ): PacketOperation =
        spawnItemDisplay(entityId, position, ItemDisplaySettings.builder().apply(settings).build())


    fun spawnTextDisplay(
        entityId: Int,
        position: FinePosition,
        settings: TextDisplaySettings
    ): PacketOperation

    fun spawnTextDisplay(
        entityId: Int,
        position: FinePosition,
        settings: TextDisplaySettings.TextDisplaySettingsBuilder<*, *>.() -> Unit
    ): PacketOperation =
        spawnTextDisplay(entityId, position, TextDisplaySettings.builder().apply(settings).build())

    fun updateSign(
        entityId: Int,
        position: BlockPosition,
        settings: SignBlockUpdateSettings
    ): PacketOperation

    fun updateSign(
        entityId: Int,
        position: BlockPosition,
        settings: SignBlockUpdateSettings.SignBlockUpdateSettingsBuilder<*, *>.() -> Unit
    ): PacketOperation =
        updateSign(entityId, position, SignBlockUpdateSettings.builder().apply(settings).build())

    fun spawnBlockDisplay(
        entityId: Int,
        position: FinePosition,
        settings: BlockDisplaySettings
    ): PacketOperation

    fun spawnBlockDisplay(
        entityId: Int,
        position: FinePosition,
        settings: BlockDisplaySettings.BlockDisplaySettingsBuilder<*, *>.() -> Unit
    ): PacketOperation =
        spawnBlockDisplay(
            entityId,
            position,
            BlockDisplaySettings.builder().apply(settings).build()
        )

    companion object {
        val instance = requiredService<SurfBukkitNmsSpawnPacketsNew>()
    }
}

@NmsUseWithCaution
val nmsSpawnPackets get() = SurfBukkitNmsSpawnPacketsNew.instance