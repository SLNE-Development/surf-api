@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges.packets.entity

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.FinePosition
import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.ints.IntSet

@NmsUseWithCaution
interface SurfPaperNmsSpawnPackets {

    fun despawn(entityIds: IntList): PacketOperation
    fun despawn(entityIds: IntSet): PacketOperation = despawn(IntArrayList(entityIds))
    fun despawn(vararg entityId: Int): PacketOperation

    fun spawnItemDisplay(
        entityId: Int,
        position: FinePosition,
        settings: ItemDisplaySettings,
    ): PacketOperation

    fun spawnItemDisplay(
        entityId: Int,
        position: FinePosition,
        settings: ItemDisplaySettings.() -> Unit,
    ): PacketOperation =
        spawnItemDisplay(entityId, position, ItemDisplaySettings.create(settings).build())


    fun spawnTextDisplay(
        entityId: Int,
        position: FinePosition,
        settings: TextDisplaySettings,
    ): PacketOperation

    fun spawnTextDisplay(
        entityId: Int,
        position: FinePosition,
        settings: TextDisplaySettings.() -> Unit,
    ): PacketOperation =
        spawnTextDisplay(entityId, position, TextDisplaySettings.create(settings).build())

    fun updateSign(
        entityId: Int,
        position: BlockPosition,
        settings: SignBlockUpdateSettings,
    ): PacketOperation

    fun updateSign(
        entityId: Int,
        position: BlockPosition,
        settings: SignBlockUpdateSettings.() -> Unit,
    ): PacketOperation =
        updateSign(entityId, position, SignBlockUpdateSettings.create(settings))

    fun spawnBlockDisplay(
        entityId: Int,
        position: FinePosition,
        settings: BlockDisplaySettings,
    ): PacketOperation

    fun spawnBlockDisplay(
        entityId: Int,
        position: FinePosition,
        settings: BlockDisplaySettings.() -> Unit,
    ): PacketOperation =
        spawnBlockDisplay(
            entityId,
            position,
            BlockDisplaySettings.create(settings).build()
        )

    fun teleport(
        entityId: Int,
        position: FinePosition,
        yaw: Float = 0f,
        pitch: Float = 0f,
        deltaMovement: FinePosition? = null,
        onGround: Boolean = false
    ): PacketOperation

    companion object : SurfPaperNmsSpawnPackets by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsSpawnPackets>()