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
     * Ensures that a vehicle tree previously captured with [captureVehicleNbt] exists in
     * [player]'s current world at the given coordinates / rotation, then force-mounts [player]
     * onto the entity identified by [directVehicleUuid].
     *
     * The tree uses its original entity UUIDs. If the root UUID is already present, the existing
     * root tree is reused as an idempotent retry only when it contains all UUIDs captured in the
     * NBT. If the root is absent but any other captured UUID already exists in the target world,
     * the restore fails to avoid partial duplicate/collision states. Newly spawned trees are
     * removed again if a later restore step fails.
     *
     * The [directVehicleUuid] is strict: it must identify the root or a passenger contained in the
     * captured vehicle tree. A missing direct vehicle is an error, returns false, and never falls
     * back to mounting on the root.
     *
     * When spawning is needed, the root position is overridden with [x]/[y]/[z] so the vehicle
     * (and therefore the mounted player) appears at a safe, caller-chosen location rather than the
     * captured crossing point.
     *
     * Must be called on the owning region/entity tick thread, after [player] is already in-world.
     *
     * @return true if the vehicle tree exists, [directVehicleUuid] was found strictly, and [player]
     *         ended up mounted on that exact entity; false for invalid NBT, UUID collisions,
     *         partial trees, missing direct vehicle, spawn failure or mount failure
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
     * Ensures that a vehicle tree previously captured with [captureVehicleNbt] exists in [world]
     * at the given coordinates / rotation **without mounting anyone**.
     *
     * This is the spawn-only counterpart of [restoreVehicleAndMount], used when several player
     * passengers are migrated as a group: the vehicle must already exist (spawned exactly once)
     * before the players are mounted in their original order with [mountPassengersInOrder].
     *
     * The tree keeps its original entity UUIDs and is idempotent for retries: if the captured root
     * UUID is already present, the call succeeds only when the existing root tree contains every
     * UUID captured in the NBT. If the root is absent but any other captured UUID already exists,
     * the call fails to avoid partial duplicate/collision states.
     *
     * Must be called on the owning region/entity tick thread.
     *
     * @return true if the complete captured vehicle tree is present after the call, whether it was
     *         newly spawned or already present; false for invalid NBT, UUID collisions, partial
     *         trees or spawn failure
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
