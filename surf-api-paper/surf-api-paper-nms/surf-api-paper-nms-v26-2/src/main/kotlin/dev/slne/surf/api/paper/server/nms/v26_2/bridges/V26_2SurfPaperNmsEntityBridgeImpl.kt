package dev.slne.surf.api.paper.server.nms.v26_2.bridges

import ca.spottedleaf.moonrise.common.PlatformHooks
import ca.spottedleaf.moonrise.common.util.TickThread
import com.mojang.brigadier.exceptions.CommandSyntaxException
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsEntityBridge
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.AdventureNBT
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNmsHolder
import dev.slne.surf.api.paper.util.chunkX
import dev.slne.surf.api.paper.util.chunkZ
import io.papermc.paper.math.FinePosition
import io.papermc.paper.util.MCUtil
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.SharedConstants
import net.minecraft.nbt.NbtUtils
import net.minecraft.server.MinecraftServer
import net.minecraft.server.commands.SummonCommand
import net.minecraft.util.ProblemReporter
import net.minecraft.util.datafix.fixes.References
import net.minecraft.world.entity.EntitySpawnReason
import net.minecraft.world.entity.EntitySpawnRequest
import net.minecraft.world.entity.Pose.CODEC
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import org.bukkit.World
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import java.util.*
import kotlin.jvm.optionals.getOrNull
import net.minecraft.world.entity.Entity as NmsEntity
import net.minecraft.world.entity.EntityType as NmsEntityType

@NmsUseWithCaution
@Suppress("ClassName")
class V26_2SurfPaperNmsEntityBridgeImpl : SurfPaperNmsEntityBridge {

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
        TickThread.ensureTickThread(nmsEntity, "Cannot capture vehicle NBT asynchronously")

        return ProblemReporter.ScopedCollector(VEHICLE_LOGGER).use { reporter ->
            val output = TagValueOutput.createWithContext(reporter, nmsEntity.registryAccess())
            nmsEntity.save(output)

            val additions = output.child("surf-api-addtions")
            nmsEntity.passengersAndSelf
                .filter { it.type.canSerialize() }
                .forEach { entity ->
                    val tag = additions.child(entity.uuid.toString())

                    tag.store("Pose", CODEC, entity.pose)

                    val living = entity.asLivingEntity()
                    if (living != null) {
                        tag.putInt("ArrowCount", living.arrowCount)
                        tag.putInt("Stingers", living.stingerCount)
                    }
                }

            MCUtil.serializeTagToBytes(output.buildResult())
        }
    }

    override fun restoreVehicle(
        world: World,
        nbt: ByteArray,
        x: Double,
        y: Double,
        z: Double,
        yaw: Float,
        pitch: Float
    ): Entity? {
        val level = world.toNms()
        TickThread.ensureTickThread(level, x, z, "Cannot restore vehicle asynchronously")

        var tag = MCUtil.deserializeTagFromBytes(nbt)
        val dataVersion = NbtUtils.getDataVersion(tag, 0)
        tag = PlatformHooks.get().convertNBT(
            References.ENTITY,
            MinecraftServer.getServer().fixerUpper,
            tag,
            dataVersion,
            SharedConstants.getCurrentVersion().dataVersion().version
        )

        val entity = ProblemReporter.ScopedCollector(VEHICLE_LOGGER).use { reporter ->
            val input = TagValueInput.create(reporter, level.registryAccess(), tag)
            val additions = input.child("surf-api-addtions").map { additionsInput ->
                require(additionsInput is TagValueInput)
                additionsInput.input.keySet()
                    .mapNotNull(fun(uuidStr: String): Pair<UUID, (NmsEntity) -> Unit>? {
                        val uuid = runCatching { UUID.fromString(uuidStr) }.getOrNull() ?: return null
                        val addition = additionsInput.child(uuidStr).getOrNull() ?: return null

                        val pose = addition.read("Pose", CODEC).getOrNull()
                        val arrowCount = addition.getInt("ArrowCount").getOrNull()
                        val stingerCount = addition.getInt("Stingers").getOrNull()

                        return uuid to { entity ->
                            if (pose != null) entity.pose = pose
                            if (arrowCount != null) entity.asLivingEntity()?.arrowCount = arrowCount
                            if (stingerCount != null) entity.asLivingEntity()?.stingerCount = stingerCount
                        }
                    })
                    .toMap()
            }.getOrNull().orEmpty()

            NmsEntityType.loadEntityRecursive(
                input,
                level,
                EntitySpawnRequest(EntitySpawnReason.LOAD, false)
            ) { entity ->
                additions[entity.uuid]?.invoke(entity)
                entity
            }
        } ?: return null

        entity.snapTo(x, y, z, yaw, pitch)

        if (!level.tryAddFreshEntityWithPassengers(entity)) {
            return null
        }

        return entity.bukkitEntity
    }

    companion object {
        private val VEHICLE_LOGGER = ComponentLogger.logger("V26_2SurfPaperNmsEntityBridgeImpl Vehicle")
    }
}
