@file:OptIn(InternalSurfApi::class)
@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.di.surfBean
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.shared.api.util.InternalSurfApi
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType

@NmsUseWithCaution
interface SurfPaperNmsEntityBridge {

    @Throws(WrapperCommandSyntaxException::class)
    fun createEntityByNbt(world: World, type: EntityType, pos: FinePosition, tag: CompoundBinaryTag)

    fun setId(entity: Entity, id: Int)

    fun getById(world: World, id: Int): Entity?

    fun captureVehicleNbt(rootVehicle: Entity): ByteArray

    fun restoreVehicle(
        world: World,
        nbt: ByteArray,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): Entity?

    companion object {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge by surfBean<SurfPaperNmsEntityBridge>()
