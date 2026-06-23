package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import ca.spottedleaf.moonrise.common.util.TickThread
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsEntityBridge
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.AdventureNBT
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNmsHolder
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.core.UUIDUtil
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.SummonCommand
import net.minecraft.server.level.ServerLevel
import net.minecraft.util.ProblemReporter
import net.minecraft.world.entity.Entity as NmsEntity
import net.minecraft.world.entity.EntityProcessor
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntityType as NmsEntityType
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.level.storage.TagValueOutput
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.event.entity.CreatureSpawnEvent
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID

@NmsUseWithCaution
class V1_21_11SurfPaperNmsEntityBridgeImpl : SurfPaperNmsEntityBridge {

    @Suppress("UnstableApiUsage")
    override fun createEntityByNbt(
        world: World,
        type: EntityType,
        pos: FinePosition,
        tag: CompoundBinaryTag
    ) {
        val worldNMS = world.toNms()
        TickThread.ensureTickThread(
            worldNMS,
            pos.chunkX,
            pos.chunkZ,
            "Cannot create entity asynchronously"
        )

        val source = MinecraftServer.getServer().createCommandSourceStack()
            .withLevel(worldNMS)

        try {
            SummonCommand.createEntity(
                source,
                type.toNmsHolder(),
                pos.toNms(),
                AdventureNBT.toNms(tag),
                false
            )
        } catch (e: CommandSyntaxException) {
            throw WrapperCommandSyntaxException(e)
        }
    }

    override fun setId(entity: Entity, id: Int) {
        entity.toNms().id = id
    }

    override fun getById(world: World, id: Int): Entity? {
        return world.toNms().getEntity(id)?.bukkitEntity
    }

    override fun captureVehicleNbt(rootVehicle: Entity): ByteArray {
        val nmsEntity = rootVehicle.toNms()
        val level = nmsEntity.level()

        val tag = CompoundTag()
        ProblemReporter.ScopedCollector(VEHICLE_LOGGER).use { reporter ->
            val output = TagValueOutput.createWrappingWithContext(reporter, level.registryAccess(), tag)
            // Saves "id" + Pos/Motion/Rotation/UUID + non-player passengers; players are excluded.
            nmsEntity.save(output)
        }

        val out = ByteArrayOutputStream()
        NbtIo.writeCompressed(tag, out)
        return out.toByteArray()
    }

    override fun restoreVehicleAndMount(
        player: Player,
        nbt: ByteArray,
        directVehicleUuid: UUID,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): Boolean {
        val nmsPlayer = player.toNms()
        val level = nmsPlayer.level()

        var spawnedRoot: NmsEntity? = null
        return try {
            val vehicleNbt = readVehicleTreeNbt(nbt) ?: return false
            if (directVehicleUuid !in vehicleNbt.entityUuids) return false

            val existingRoot = getEntityByUuid(level, vehicleNbt.rootUuid)
            val root = if (existingRoot != null) {
                if (!vehicleTreeContainsAll(existingRoot, vehicleNbt.entityUuids)) return false
                existingRoot
            } else {
                if (hasPartialUuidCollision(level, vehicleNbt.entityUuids, vehicleNbt.rootUuid)) return false

                applyTargetTransform(vehicleNbt.tag, x, y, z, yaw, pitch)
                val spawned = spawnVehicleTreeFromTag(level, vehicleNbt.tag) ?: return false
                spawnedRoot = spawned
                standUpCamels(spawned)

                if (!vehicleTreeContainsAll(spawned, vehicleNbt.entityUuids)) {
                    discardVehicleTree(spawned)
                    spawnedRoot = null
                    return false
                }

                spawned
            }

            standUpCamels(root)
            val directVehicle = findEntityInTree(root, directVehicleUuid)
            if (directVehicle == null) {
                spawnedRoot?.let(::discardVehicleTree)
                spawnedRoot = null
                return false
            }

            if (nmsPlayer.vehicle !== directVehicle) {
                if (nmsPlayer.isPassenger) {
                    nmsPlayer.stopRiding()
                }
                nmsPlayer.startRiding(directVehicle, true, false)
            }

            val mounted = nmsPlayer.vehicle === directVehicle
            if (!mounted) {
                spawnedRoot?.let(::discardVehicleTree)
                spawnedRoot = null
            }

            mounted
        } catch (_: Exception) {
            spawnedRoot?.let(::discardVehicleTree)
            false
        }
    }

    override fun spawnVehicleTree(
        world: World,
        nbt: ByteArray,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ): Boolean {
        val level = world.toNms()

        var spawnedRoot: NmsEntity? = null
        return try {
            val vehicleNbt = readVehicleTreeNbt(nbt) ?: return false

            val existingRoot = getEntityByUuid(level, vehicleNbt.rootUuid)
            if (existingRoot != null) {
                val complete = vehicleTreeContainsAll(existingRoot, vehicleNbt.entityUuids)
                if (complete) {
                    standUpCamels(existingRoot)
                }
                return complete
            }

            if (hasPartialUuidCollision(level, vehicleNbt.entityUuids, vehicleNbt.rootUuid)) return false

            applyTargetTransform(vehicleNbt.tag, x, y, z, yaw, pitch)
            val root = spawnVehicleTreeFromTag(level, vehicleNbt.tag) ?: return false
            spawnedRoot = root
            standUpCamels(root)

            val complete = vehicleTreeContainsAll(root, vehicleNbt.entityUuids)
            if (!complete) {
                discardVehicleTree(root)
                spawnedRoot = null
            }

            complete
        } catch (_: Exception) {
            spawnedRoot?.let(::discardVehicleTree)
            false
        }
    }

    override fun mountPassengersInOrder(vehicle: Entity, orderedPassengers: List<Entity>): Boolean {
        val nmsVehicle = vehicle.toNms()
        val nmsPassengers = orderedPassengers.map { it.toNms() }

        // Clear the current passenger list first so the order is fully controlled by us.
        for (passenger in nmsVehicle.passengers.toList()) {
            passenger.stopRiding()
        }

        // Re-mount in order; the server appends each passenger, so the resulting order matches.
        for (passenger in nmsPassengers) {
            if (passenger.vehicle !== null && passenger.vehicle !== nmsVehicle) {
                passenger.stopRiding()
            }
            passenger.startRiding(nmsVehicle, true, false)
            if (passenger.vehicle !== nmsVehicle) return false
        }

        return nmsPassengers.isNotEmpty() && nmsVehicle.passengers.containsAll(nmsPassengers)
    }

    private fun readVehicleTreeNbt(nbt: ByteArray): VehicleTreeNbt? {
        val tag = try {
            NbtIo.readCompressed(ByteArrayInputStream(nbt), NbtAccounter.unlimitedHeap())
        } catch (_: Exception) {
            return null
        }

        val entityUuids = LinkedHashSet<UUID>()
        if (!collectVehicleUuids(tag, entityUuids)) return null

        return VehicleTreeNbt(
            tag = tag,
            rootUuid = entityUuids.firstOrNull() ?: return null,
            entityUuids = entityUuids,
        )
    }

    private fun collectVehicleUuids(tag: CompoundTag, entityUuids: MutableSet<UUID>): Boolean {
        val uuid = tag.entityUuidOrNull() ?: return false
        if (!entityUuids.add(uuid)) return false

        val passengers = tag.getList(NmsEntity.TAG_PASSENGERS).orElse(null)
        if (passengers == null) {
            return !tag.contains(NmsEntity.TAG_PASSENGERS)
        }

        for (i in 0 until passengers.size) {
            val passengerTag = passengers.getCompound(i).orElse(null) ?: return false
            if (!collectVehicleUuids(passengerTag, entityUuids)) return false
        }

        return true
    }

    private fun CompoundTag.entityUuidOrNull(): UUID? {
        val uuid = getIntArray(NmsEntity.TAG_UUID).orElse(null) ?: return null
        if (uuid.size != 4) return null

        return UUIDUtil.uuidFromIntArray(uuid)
    }

    private fun getEntityByUuid(level: ServerLevel, uuid: UUID): NmsEntity? {
        return level.entities.get(uuid)
    }

    private fun hasPartialUuidCollision(
        level: ServerLevel,
        entityUuids: Set<UUID>,
        rootUuid: UUID,
    ): Boolean {
        for (uuid in entityUuids) {
            if (uuid == rootUuid) continue
            if (getEntityByUuid(level, uuid) != null) return true
        }

        return false
    }

    private fun vehicleTreeContainsAll(root: NmsEntity, entityUuids: Set<UUID>): Boolean {
        val existingUuids = LinkedHashSet<UUID>()
        existingUuids.add(root.uuid)
        for (passenger in root.indirectPassengers) {
            existingUuids.add(passenger.uuid)
        }

        return existingUuids.containsAll(entityUuids)
    }

    private fun findEntityInTree(root: NmsEntity, uuid: UUID): NmsEntity? {
        if (root.uuid == uuid) return root
        return root.indirectPassengers.firstOrNull { it.uuid == uuid }
    }

    private fun applyTargetTransform(
        tag: CompoundTag,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float,
    ) {
        // Override the captured crossing position with the safe target position so the vehicle
        // does not re-appear on the border.
        tag.put(NmsEntity.TAG_POS, doubleList(x, y, z))
        tag.put(NmsEntity.TAG_ROTATION, floatList(yaw, pitch))
    }

    private fun spawnVehicleTreeFromTag(level: ServerLevel, tag: CompoundTag): NmsEntity? {
        return NmsEntityType.loadEntityRecursive(
            tag,
            level,
            EntitySpawnReason.LOAD,
            EntityProcessor { entity ->
                if (level.addWithUUID(entity, CreatureSpawnEvent.SpawnReason.MOUNT)) entity else null
            }
        )
    }

    private fun discardVehicleTree(root: NmsEntity) {
        val entities = ArrayList<NmsEntity>()
        entities.add(root)
        entities.addAll(root.indirectPassengers)

        for (entity in entities.asReversed()) {
            entity.discard()
        }
    }

    /**
     * Camels encode their (sitting) pose as an absolute game-time tick. After migrating to a shard
     * with a different game time that tick lies in the past/future, leaving the camel stuck in a
     * sit/stand transition (rendered lying down). Re-stand any migrated camel so its pose tick is
     * rebased onto this shard's game time.
     */
    private fun standUpCamels(root: NmsEntity) {
        if (root is Camel) runCatching { root.standUpInstantly() }
        for (passenger in root.indirectPassengers) {
            if (passenger is Camel) runCatching { passenger.standUpInstantly() }
        }
    }

    private fun doubleList(x: Double, y: Double, z: Double) = ListTag().apply {
        add(DoubleTag.valueOf(x))
        add(DoubleTag.valueOf(y))
        add(DoubleTag.valueOf(z))
    }

    private fun floatList(yaw: Float, pitch: Float) = ListTag().apply {
        add(FloatTag.valueOf(yaw))
        add(FloatTag.valueOf(pitch))
    }

    companion object {
        private val VEHICLE_LOGGER = ComponentLogger.logger("SurfPaperNmsEntityBridge Vehicle")
    }

    private data class VehicleTreeNbt(
        val tag: CompoundTag,
        val rootUuid: UUID,
        val entityUuids: Set<UUID>,
    )
}
