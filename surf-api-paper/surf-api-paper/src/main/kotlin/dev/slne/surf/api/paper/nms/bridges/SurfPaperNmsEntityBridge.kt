@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.nms.bridges

import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import java.util.UUID

@NmsUseWithCaution
interface SurfPaperNmsEntityBridge {

    @Throws(WrapperCommandSyntaxException::class)
    fun createEntityByNbt(world: World, type: EntityType, pos: FinePosition, tag: CompoundBinaryTag)

    fun setId(entity: Entity, id: Int)

    fun getById(world: World, id: Int): Entity?

    /**
     * Serializes [rootVehicle] – which must be the *root* of a ride tree – together with its
     * non-player passenger subtree into a portable, compressed NBT blob that can be migrated to
     * another server.
     *
     * Player passengers are excluded automatically (players are not serialized as passengers).
     * Entity UUIDs, position, rotation, velocity, metadata and any non-player passengers are
     * preserved so the tree can be faithfully recreated elsewhere with [restoreVehicleAndMount].
     *
     * Must be called on the owning region/entity tick thread.
     *
     * @param rootVehicle the root entity of the ride tree (e.g. the boat the player sits in)
     * @return the gzip-compressed NBT representation of the tree
     */
    fun captureVehicleNbt(rootVehicle: Entity): ByteArray

    /**
     * Recreates a vehicle tree previously captured with [captureVehicleNbt] in [player]'s current
     * world at the given coordinates / rotation, then force-mounts [player] onto the entity
     * identified by [directVehicleUuid].
     *
     * The tree is recreated with its original entity UUIDs using add-with-uuid semantics, so a
     * duplicate (an entity whose UUID is already present in the world) is rejected instead of
     * being spawned a second time. The root position is overridden with [x]/[y]/[z] so the
     * vehicle (and therefore the mounted player) appears at a safe, caller-chosen location rather
     * than the captured crossing point.
     *
     * Must be called on the owning region/entity tick thread, after [player] is already in-world.
     *
     * @return true if [player] ended up mounted on the recreated vehicle, false otherwise
     */
    fun restoreVehicleAndMount(
        player: Player,
        nbt: ByteArray,
        directVehicleUuid: UUID,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): Boolean

    /**
     * Recreates a vehicle tree previously captured with [captureVehicleNbt] in [world] at the given
     * coordinates / rotation **without mounting anyone**.
     *
     * This is the spawn-only counterpart of [restoreVehicleAndMount], used when several player
     * passengers are migrated as a group: the vehicle must already exist (spawned exactly once)
     * before the players are mounted in their original order with [mountPassengersInOrder]. The
     * tree keeps its original entity UUIDs (add-with-uuid rejects duplicates), so calling this
     * twice for the same group is a no-op for the second call.
     *
     * Must be called on the owning region/entity tick thread.
     *
     * @return true if the root entity was spawned (or already present), false on failure
     */
    fun spawnVehicleTree(
        world: World,
        nbt: ByteArray,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): Boolean

    /**
     * Rebuilds [vehicle]'s passenger list to exactly [orderedPassengers], in that order.
     *
     * All current passengers are dismounted first, then each entity in [orderedPassengers] is
     * force-mounted in turn. Because the server appends each new passenger, the resulting passenger
     * order matches the list, which for boats decides the controlling passenger (index 0). This is
     * how the original driver and seating order are restored after a multi-passenger migration.
     *
     * Must be called on the owning region/entity tick thread, with every entity already in-world.
     *
     * @return true if [vehicle] ended up with at least one passenger
     */
    fun mountPassengersInOrder(vehicle: Entity, orderedPassengers: List<Entity>): Boolean

    companion object : SurfPaperNmsEntityBridge by bridge {
        val INSTANCE get() = bridge
    }
}

@OptIn(NmsUseWithCaution::class)
private val bridge = requiredService<SurfPaperNmsEntityBridge>()