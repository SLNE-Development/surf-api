package dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.entity

import com.google.common.flogger.StackSize
import com.mojang.math.Transformation
import dev.slne.surf.api.core.util.logger
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.entity.*
import dev.slne.surf.api.paper.server.nms.v26_2.bridges.packets.V26_2PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v26_2.extensions.toNms
import io.papermc.paper.math.BlockPosition
import io.papermc.paper.math.FinePosition
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.core.HolderLookup
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtOps
import net.minecraft.network.protocol.game.*
import net.minecraft.server.MinecraftServer
import net.minecraft.world.entity.Display
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.EntityTypes
import net.minecraft.world.entity.PositionMoveRotation
import net.minecraft.world.level.block.entity.BlockEntityTypes
import net.minecraft.world.level.block.entity.SignText
import net.minecraft.world.phys.Vec3
import org.bukkit.entity.TextDisplay.TextAlignment
import kotlin.experimental.and
import kotlin.experimental.inv
import kotlin.experimental.or

@Suppress("UnstableApiUsage", "ClassName")
@NmsUseWithCaution
class V26_2SurfPaperNmsSpawnPacketsImpl : SurfPaperNmsSpawnPackets {
    private val log = logger()

    override fun despawn(entityIds: IntList) =
        V26_2PacketOperationImpl.simple { ClientboundRemoveEntitiesPacket(entityIds) }

    override fun despawn(vararg entityId: Int) =
        V26_2PacketOperationImpl.simple { ClientboundRemoveEntitiesPacket(*entityId) }


    override fun spawnItemDisplay(
        entityId: Int,
        position: FinePosition,
        settings: ItemDisplaySettings,
    ) = V26_2PacketOperationImpl.complex { player, packets ->
        val serverPlayer = player.toNms()
        val display = Display.ItemDisplay(EntityTypes.ITEM_DISPLAY, serverPlayer.level()).apply {
            id = entityId

            setPosition(position)
            applySettings(settings)

            itemStack = settings.itemStack.toNms()
            itemTransform = settings.itemDisplayTransform.toNms()
        }

        packets.add(ClientboundAddEntityPacket(display, 0, display.blockPosition()))
        packets.add(createSetEntityDataPacket(entityId, display))
        packets
    }

    override fun spawnTextDisplay(
        entityId: Int,
        position: FinePosition,
        settings: TextDisplaySettings,
    ) = V26_2PacketOperationImpl.complex { player, packets ->
        val serverPlayer = player.toNms()
        val display = Display.TextDisplay(EntityTypes.TEXT_DISPLAY, serverPlayer.level()).apply {
            id = entityId

            setPosition(position)
            applySettings(settings)

            text = settings.text.toNms()

            val data = getEntityData()
            data[Display.TextDisplay.DATA_LINE_WIDTH_ID] = settings.lineWidth
            data[Display.TextDisplay.DATA_BACKGROUND_COLOR_ID] = settings.backgroundColor.value()

            when (settings.textAlignment) {
                TextAlignment.CENTER -> {
                    setFlag(Display.TextDisplay.FLAG_ALIGN_LEFT, false)
                    setFlag(Display.TextDisplay.FLAG_ALIGN_RIGHT, false)
                }

                TextAlignment.LEFT -> {
                    setFlag(Display.TextDisplay.FLAG_ALIGN_LEFT, true)
                    setFlag(Display.TextDisplay.FLAG_ALIGN_RIGHT, false)
                }

                TextAlignment.RIGHT -> {
                    setFlag(Display.TextDisplay.FLAG_ALIGN_LEFT, false)
                    setFlag(Display.TextDisplay.FLAG_ALIGN_RIGHT, true)
                }
            }
        }


        packets.add(ClientboundAddEntityPacket(display, 0, display.blockPosition()))
        packets.add(createSetEntityDataPacket(entityId, display))
        packets
    }

    override fun updateSign(
        entityId: Int,
        position: BlockPosition,
        settings: SignBlockUpdateSettings,
    ) = V26_2PacketOperationImpl.complex { player, packets ->
        val nbt = CompoundTag()
        val registryLookup = MinecraftServer.getServer().registryAccess()
        writeUpdateSignToTag(nbt, registryLookup, settings.frontText, settings.backText)

        packets.add(ClientboundBlockEntityDataPacket(position.toNms(), BlockEntityTypes.SIGN, nbt))
        packets
    }

    override fun spawnBlockDisplay(
        entityId: Int,
        position: FinePosition,
        settings: BlockDisplaySettings,
    ) = V26_2PacketOperationImpl.complex { player, packets ->
        val serverPlayer = player.toNms()
        val display = Display.BlockDisplay(EntityTypes.BLOCK_DISPLAY, serverPlayer.level()).apply {
            id = entityId

            setPosition(position)
            applySettings(settings)
            blockState = settings.blockData.toNms()
        }

        packets.add(ClientboundAddEntityPacket(display, 0, display.blockPosition()))
        packets.add(createSetEntityDataPacket(entityId, display))
        packets
    }

    override fun teleport(
        entityId: Int,
        position: FinePosition,
        yaw: Float,
        pitch: Float,
        deltaMovement: FinePosition?,
        onGround: Boolean,
    ) = V26_2PacketOperationImpl.simple {
        ClientboundTeleportEntityPacket.teleport(
            entityId,
            PositionMoveRotation(position.toNms(), deltaMovement?.toNms() ?: Vec3.ZERO, yaw, pitch),
            emptySet(),
            onGround
        )
    }

    private fun createSetEntityDataPacket(entityId: Int, entity: Entity) =
        ClientboundSetEntityDataPacket(entityId, entity.getEntityData().packAll())

    private fun writeUpdateSignToTag(
        nbt: CompoundTag,
        registryLookup: HolderLookup.Provider,
        frontText: SignBlockUpdateSettings.SignText,
        backText: SignBlockUpdateSettings.SignText,
    ) {
        writeTextToTag(nbt, registryLookup, frontText, "front_text", true)
        writeTextToTag(nbt, registryLookup, backText, "back_text", false)
    }

    private fun writeTextToTag(
        nbt: CompoundTag,
        registryLookup: HolderLookup.Provider,
        text: SignBlockUpdateSettings.SignText,
        tagText: String,
        isFrontText: Boolean,
    ) {
        val nbtOps = registryLookup.createSerializationContext(NbtOps.INSTANCE)
        val textTag = SignText.DIRECT_CODEC.encodeStart(nbtOps, text.toNms())
        textTag.resultOrPartial { logFailedEncodeText(it, isFrontText) }
            .ifPresent { nbt.put(tagText, it) }
    }

    private fun logFailedEncodeText(string: String, front: Boolean) {
        log.atSevere()
            .withStackTrace(StackSize.MEDIUM)
            .atMostEvery(5, java.util.concurrent.TimeUnit.SECONDS)
            .log("Failed to encode %s text: %s", if (front) "front" else "back", string)
    }

    private fun getTransformation(settings: DisplaySettings) = Transformation(
        settings.translation.toNms(),
        settings.leftRotation.toNms(),
        settings.scale.toNms(),
        settings.rightRotation.toNms()
    )

    private fun Display.applySettings(settings: DisplaySettings) {
        xRot = settings.pitch
        yRot = settings.yaw
        setTransformation(getTransformation(settings))
        billboardConstraints = settings.billboardConstraints.toNms()
    }

    private fun Entity.setPosition(position: FinePosition) {
        setPosRaw(position.x(), position.y(), position.z())
    }

    private fun Display.TextDisplay.setFlag(flag: Byte, set: Boolean) {
        flags = if (set) flags or flag else flags and flag.inv()
    }
}
