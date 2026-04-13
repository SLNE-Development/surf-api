package dev.slne.surf.api.paper.server.display

import com.github.retrooper.packetevents.protocol.attribute.Attributes
import com.github.retrooper.packetevents.protocol.component.ComponentTypes
import com.github.retrooper.packetevents.protocol.component.builtin.item.ItemModel
import com.github.retrooper.packetevents.protocol.entity.data.EntityData
import com.github.retrooper.packetevents.protocol.entity.data.EntityDataTypes
import com.github.retrooper.packetevents.protocol.entity.type.EntityTypes
import com.github.retrooper.packetevents.protocol.item.ItemStack
import com.github.retrooper.packetevents.protocol.item.type.ItemTypes
import com.github.retrooper.packetevents.protocol.player.*
import com.github.retrooper.packetevents.protocol.potion.PotionTypes
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState
import com.github.retrooper.packetevents.protocol.world.states.type.StateTypes
import com.github.retrooper.packetevents.resources.ResourceLocation
import com.github.retrooper.packetevents.util.Vector3d
import com.github.retrooper.packetevents.util.Vector3i
import com.github.retrooper.packetevents.wrapper.play.server.*
import dev.slne.surf.api.paper.server.display.cursor.Cursor
import dev.slne.surf.api.paper.server.display.user.DisplayUser
import net.kyori.adventure.text.Component
import org.bukkit.GameMode
import java.util.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * Manages a display session for a player.
 *
 * A session mounts the player on an invisible horse for yaw/pitch cursor tracking,
 * creates a fake player entity as a camera, and applies visual effects (invisibility,
 * empty inventory) to create a clean display viewing experience.
 *
 * Lifecycle: [open] → player interacts with display → [close] restores original state.
 */
class DisplaySession(
    val user: DisplayUser,
    private val centerYaw: Float = 0f,
    private val cameraEyePosition: Vector3d? = null
) {
    val horseEntityId = nextEntityId()
    val fakePlayerEntityId = CAMERA_ENTITY_ID

    var isActive = false
        private set

    lateinit var cursor: Cursor
        private set

    private var initialYaw = 0f
    private var initialPitch = 0f
    private var initialGameMode = GameMode.SURVIVAL
    private var initialPosition = Vector3d.zero()

    var copperGratePos = Vector3i.zero()
        internal set

    var cursorX = 0
        internal set
    var cursorY = 0
        internal set

    fun open() {
        val player = user.player ?: return

        initialYaw = player.location.yaw
        initialPitch = player.location.pitch
        initialGameMode = player.gameMode
        initialPosition = Vector3d(player.location.x, player.location.y, player.location.z)

        val eyeLoc = player.eyeLocation
        val playerEyePos = Vector3d(eyeLoc.x, eyeLoc.y, eyeLoc.z)
        val cameraPos = cameraEyePosition ?: playerEyePos

        cursor = Cursor(horseEntityId, user)
        spawnHorse(playerEyePos)
        mountPlayer()
        spawnFakePlayerAndCamera(cameraPos, player)
        applyVisualEffects(player)

        isActive = true
    }

    fun close() {
        if (!isActive) return
        isActive = false
        val player = user.player ?: return

        user.sendPacket(WrapperPlayServerCamera(player.entityId))
        user.sendPacket(WrapperPlayServerDestroyEntities(horseEntityId))
        user.sendPacket(WrapperPlayServerDestroyEntities(fakePlayerEntityId))

        val gmValue = when (initialGameMode) {
            GameMode.SURVIVAL -> 0
            GameMode.CREATIVE -> 1
            GameMode.ADVENTURE -> 2
            GameMode.SPECTATOR -> 3
        }
        user.sendPacket(
            WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
                gmValue.toFloat()
            )
        )

        player.isInvisible = false
        user.sendPacket(WrapperPlayServerRemoveEntityEffect(player.entityId, PotionTypes.INVISIBILITY))

        user.sendPacket(
            WrapperPlayServerPlayerPositionAndLook(
                initialPosition.x, initialPosition.y, initialPosition.z,
                initialYaw, initialPitch,
                0.toByte(), 0, false
            )
        )

        user.sendPacket(
            WrapperPlayServerBlockChange(
                copperGratePos,
                WrappedBlockState.getDefaultState(StateTypes.AIR)
            )
        )

        user.sendPacket(
            WrapperPlayServerTimeUpdate(
                player.world.gameTime,
                player.playerTime
            )
        )

        player.updateInventory()
    }

    private fun spawnHorse(pos: Vector3d) {
        val spawnPacket = WrapperPlayServerSpawnEntity(
            horseEntityId,
            Optional.of(UUID.randomUUID()),
            EntityTypes.HORSE,
            pos,
            0f,
            0f,
            0f,
            0,
            Optional.of(Vector3d.zero())
        )
        user.sendPacket(spawnPacket)

        val metadata = WrapperPlayServerEntityMetadata(
            horseEntityId,
            listOf(
                EntityData(0, EntityDataTypes.BYTE, (0x20 or 0x02).toByte()),
                EntityData(17, EntityDataTypes.BYTE, 0x04.toByte()),
            )
        )
        user.sendPacket(metadata)

        val attributes = listOf(
            WrapperPlayServerUpdateAttributes.Property(
                Attributes.JUMP_STRENGTH,
                0.0,
                listOf(
                    WrapperPlayServerUpdateAttributes.PropertyModifier(
                        UUID.randomUUID(), 0.0,
                        WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE
                    )
                )
            ),
            WrapperPlayServerUpdateAttributes.Property(
                Attributes.SCALE,
                0.01,
                listOf(
                    WrapperPlayServerUpdateAttributes.PropertyModifier(
                        UUID.randomUUID(), 0.0,
                        WrapperPlayServerUpdateAttributes.PropertyModifier.Operation.MULTIPLY_BASE
                    )
                )
            )
        )
        user.sendPacket(WrapperPlayServerUpdateAttributes(horseEntityId, attributes))

        user.sendPacket(
            WrapperPlayServerEntityTeleport(
                horseEntityId,
                Vector3d(pos.x, pos.y - 1.7, pos.z),
                0f, 180f, false
            )
        )

        cursor.sendCursorUpdate()
    }

    private fun mountPlayer() {
        val player = user.player ?: return

        user.sendPacket(WrapperPlayServerPlayerRotation(centerYaw, 0f))

        user.sendPacket(
            WrapperPlayServerSetPassengers(
                horseEntityId,
                intArrayOf(player.entityId)
            )
        )

        user.sendPacket(WrapperPlayServerPlayerRotation(centerYaw, 0f))
    }

    private fun spawnFakePlayerAndCamera(pos: Vector3d, player: org.bukkit.entity.Player) {
        val uuid = UUID.randomUUID()

        val properties = mutableListOf<TextureProperty>()
        for (property in player.playerProfile.properties) {
            properties.add(TextureProperty(property.name, property.value, property.signature))
        }

        val profile = UserProfile(uuid, player.name, properties)

        user.sendPacket(
            WrapperPlayServerPlayerInfoUpdate(
                WrapperPlayServerPlayerInfoUpdate.Action.ADD_PLAYER,
                WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                    profile, false, 0, com.github.retrooper.packetevents.protocol.player.GameMode.CREATIVE,
                    null, null, 0, true
                )
            )
        )

        val feetPos = Vector3d(pos.x, pos.y - 1.62, pos.z)
        user.sendPacket(
            WrapperPlayServerSpawnEntity(
                fakePlayerEntityId,
                uuid,
                EntityTypes.PLAYER,
                com.github.retrooper.packetevents.protocol.world.Location(
                    feetPos,
                    centerYaw,
                    0f
                ),
                centerYaw,
                0,
                Vector3d.zero()
            )
        )

        user.sendPacket(WrapperPlayServerCamera(fakePlayerEntityId))

        copperGratePos = Vector3i(
            kotlin.math.floor(pos.x).toInt(),
            kotlin.math.floor(pos.y).toInt(),
            kotlin.math.floor(pos.z).toInt()
        )
        user.sendPacket(
            WrapperPlayServerBlockChange(
                copperGratePos,
                WrappedBlockState.getDefaultState(StateTypes.EXPOSED_COPPER_GRATE)
            )
        )
    }

    private fun applyVisualEffects(player: org.bukkit.entity.Player) {
        player.isInvisible = true
        user.sendPacket(
            WrapperPlayServerEntityEffect(
                player.entityId,
                PotionTypes.INVISIBILITY,
                255,
                -1,
                0.toByte()
            )
        )

        val emptyItem = ItemStack.builder()
            .type(ItemTypes.TRIDENT)
            .component(ComponentTypes.ITEM_NAME, Component.empty())
            .component(ComponentTypes.ITEM_MODEL, ItemModel(ResourceLocation.minecraft("air")))
            .build()
        user.sendPacket(
            WrapperPlayServerWindowItems(
                0, 0,
                java.util.Collections.nCopies(44, emptyItem),
                emptyItem
            )
        )
        user.sendPacket(WrapperPlayServerSetSlot(0, 0, 45, emptyItem))

        user.sendPacket(
            WrapperPlayServerChangeGameState(
                WrapperPlayServerChangeGameState.Reason.CHANGE_GAME_MODE,
                0f
            )
        )
    }

    companion object {
        private const val CAMERA_ENTITY_ID = -10_000
        private val entityIdCounter = AtomicInteger(2_000_000)
        fun nextEntityId() = entityIdCounter.getAndIncrement()
    }
}
