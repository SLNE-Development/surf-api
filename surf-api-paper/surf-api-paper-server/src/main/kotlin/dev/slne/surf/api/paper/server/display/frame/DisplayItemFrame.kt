package dev.slne.surf.api.paper.server.display.frame

import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerDestroyEntities
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityMetadata
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSpawnEntity
import dev.slne.surf.api.paper.server.display.map.DisplayMap
import dev.slne.surf.api.paper.server.display.user.DisplayUser
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

class DisplayItemFrame(
    val location: Vector3d,
    val map: DisplayMap,
    val facing: Direction = Direction.SOUTH
) {
    val entityId = nextEntityId()

    fun spawn(user: DisplayUser) {
        user.sendPacket(map.createPacket())
        user.sendPacket(createSpawnPacket())
        user.sendPacket(createMetadataPacket())
    }

    fun despawn(user: DisplayUser) {
        user.sendPacket(WrapperPlayServerDestroyEntities(entityId))
    }

    fun sendMapUpdate(user: DisplayUser) {
        user.sendPacket(map.createPacket())
    }

    private fun createSpawnPacket(): WrapperPlayServerSpawnEntity {
        return WrapperPlayServerSpawnEntity(
            entityId,
            Optional.of(UUID.randomUUID()),
            EntityTypes.ITEM_FRAME,
            location,
            0.0f,
            0.0f,
            0.0f,
            facing.value,
            Optional.empty()
        )
    }

    private fun createMetadataPacket(): WrapperPlayServerEntityMetadata {
        val itemStack = ItemStack.builder()
            .type(ItemTypes.FILLED_MAP)
            .component(ComponentTypes.MAP_ID, map.mapId)
            .build()

        return WrapperPlayServerEntityMetadata(
            entityId,
            listOf(
                EntityData(0, EntityDataTypes.BYTE, 0x20.toByte()),
                EntityData(9, EntityDataTypes.ITEMSTACK, itemStack),
            )
        )
    }

    enum class Direction(val value: Int) {
        DOWN(0), UP(1), NORTH(2), SOUTH(3), WEST(4), EAST(5);

        val opposite: Direction
            get() = when (this) {
                DOWN -> UP
                UP -> DOWN
                NORTH -> SOUTH
                SOUTH -> NORTH
                WEST -> EAST
                EAST -> WEST
            }
    }

    companion object {
        private val entityIdCounter = AtomicInteger(1_000_000)
        fun nextEntityId() = entityIdCounter.getAndIncrement()
    }
}
