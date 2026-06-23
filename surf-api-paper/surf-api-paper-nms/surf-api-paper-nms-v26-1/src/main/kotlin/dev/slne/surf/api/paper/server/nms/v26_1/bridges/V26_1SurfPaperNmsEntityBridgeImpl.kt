package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import ca.spottedleaf.moonrise.common.util.TickThread
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsEntityBridge
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.AdventureNBT
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNmsHolder
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import io.papermc.paper.math.FinePosition
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.DoubleTag
import net.minecraft.nbt.FloatTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.NbtAccounter
import net.minecraft.nbt.NbtIo
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.SummonCommand
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
@Suppress("ClassName")
class V26_1SurfPaperNmsEntityBridgeImpl : SurfPaperNmsEntityBridge {

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

        val tag = NbtIo.readCompressed(ByteArrayInputStream(nbt), NbtAccounter.unlimitedHeap())
        // Override the captured crossing position with the safe target position so the vehicle (and
        // therefore the mounted player) does not re-appear on the border.
        tag.put("Pos", doubleList(x, y, z))
        tag.put("Rotation", floatList(yaw, pitch))

        val root = NmsEntityType.loadEntityRecursive(
            tag,
            level,
            EntitySpawnReason.LOAD,
            EntityProcessor { entity ->
                // add-with-uuid rejects entities whose uuid is already present -> built-in dedup.
                if (level.addWithUUID(entity, CreatureSpawnEvent.SpawnReason.MOUNT)) entity else null
            }
        ) ?: return false
        standUpCamels(root)

        val directVehicle = if (root.uuid == directVehicleUuid) {
            root
        } else {
            root.indirectPassengers.firstOrNull { it.uuid == directVehicleUuid } ?: root
        }

        nmsPlayer.startRiding(directVehicle, true, false)
        return nmsPlayer.isPassenger
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

        val tag = NbtIo.readCompressed(ByteArrayInputStream(nbt), NbtAccounter.unlimitedHeap())
        tag.put("Pos", doubleList(x, y, z))
        tag.put("Rotation", floatList(yaw, pitch))

        val root = NmsEntityType.loadEntityRecursive(
            tag,
            level,
            EntitySpawnReason.LOAD,
            EntityProcessor { entity ->
                if (level.addWithUUID(entity, CreatureSpawnEvent.SpawnReason.MOUNT)) entity else null
            }
        ) ?: return false
        standUpCamels(root)
        return true
    }

    override fun mountPassengersInOrder(vehicle: Entity, orderedPassengers: List<Entity>): Boolean {
        val nmsVehicle = vehicle.toNms()

        // Clear the current passenger list first so the order is fully controlled by us.
        for (passenger in nmsVehicle.passengers.toList()) {
            passenger.stopRiding()
        }

        // Re-mount in order; the server appends each passenger, so the resulting order matches.
        for (passenger in orderedPassengers) {
            passenger.toNms().startRiding(nmsVehicle, true, false)
        }

        return nmsVehicle.passengers.isNotEmpty()
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
}
