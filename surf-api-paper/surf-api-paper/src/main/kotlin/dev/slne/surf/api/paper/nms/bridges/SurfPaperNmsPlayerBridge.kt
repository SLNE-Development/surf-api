package dev.slne.surf.api.paper.nms.bridges

import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge.Companion.editOfflineInventory
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatMessageMirror
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatSessionSnapshot
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus
import java.io.File
import java.nio.file.Path

@NmsUseWithCaution
@ApiStatus.NonExtendable
interface SurfPaperNmsPlayerBridge {

    fun removeAllTrackedEntities(player: Player, swallowExceptions: Boolean = true)
    fun removeAllTrackedPlayers(player: Player, swallowExceptions: Boolean = true)

    /**
     * Re-sends the full client-side tracking and passenger state for [player]'s own vehicle
     * chain so that the client and the server agree again on what the player is riding.
     *
     * This exists for *seamless* server moves (e.g. shard transfers) where the client keeps its
     * existing PLAY-state world because the login/respawn/reconfiguration packets are suppressed
     * upstream. On the target server the vehicle is re-created from the player's persisted
     * `RootVehicle` data, but with a **fresh network entity id** and a freshly initialised entity
     * tracker. The relationship packet ([net.minecraft.network.protocol.game.ClientboundSetPassengersPacket])
     * is normally only emitted when the passenger list *changes*, so after a seamless move the
     * client can be left believing it is (not) riding while the server believes the opposite.
     *
     * For every entity in the player's vehicle tree (the root vehicle and all of its
     * (indirect) passengers, except the player itself) this destroys and immediately re-creates
     * the client pairing — i.e. it sends a remove packet followed by the full add-pairing bundle
     * (spawn, metadata, equipment, attributes and the passenger links). Afterwards it re-asserts
     * every passenger link once more, after all involved entities are guaranteed to exist on the
     * client, so that stacked vehicles resolve in the correct order.
     *
     * Must be called on the owning region/entity tick thread.
     *
     * @param player the transferred player whose riding state should be reconciled
     * @param swallowExceptions when true, per-entity failures are ignored instead of propagated
     * @return the number of vehicle-chain entities that were resynced; `0` when the player is
     *         neither riding anything nor carrying any passengers (nothing to do)
     */
    fun resyncVehicleState(player: Player, swallowExceptions: Boolean = true): Int

    /**
     * Re-pairs a single [entity] for [viewer] so the viewer's client holds the entity with the
     * server's *current* network id, metadata and passenger links.
     *
     * This destroys the existing client pairing and re-creates it (remove packet followed by the
     * full add-pairing bundle). If the viewer was not yet part of the entity tracker's seen-by
     * set it is added, so subsequent incremental updates keep flowing. Useful to repair an
     * individual entity whose id drifted after a seamless server move (which otherwise breaks
     * interaction packets that reference the network id).
     *
     * [viewer] and [entity] must be in the same world/NMS level. Cross-world calls return false
     * before consulting the viewer level's tracker map, because numeric entity ids can collide
     * between levels.
     *
     * Must be called on the owning region/entity tick thread.
     *
     * @param viewer the player that should receive the refreshed entity
     * @param entity the entity to re-pair for the viewer
     * @param swallowExceptions when true, failures are ignored instead of propagated
     * @return true if the entity had an active tracker and was resynced, false otherwise
     */
    fun resyncEntityForViewer(viewer: Player, entity: Entity, swallowExceptions: Boolean = true): Boolean

    /**
     * Re-sends the connection-bound player state that a normal respawn would refresh but that is
     * lost during a seamless server move, namely the player's abilities (fly/allow-fly/walk and
     * fly speed). Safe to call repeatedly; it is a no-op when the player has no connection.
     *
     * Must be called on the owning region/entity tick thread.
     */
    fun resyncPlayerState(player: Player)

    fun getRemoteChatSessionData(player: Player): RemoteChatSessionData?

    fun createChatSessionSnapshot(player: Player): PlayerChatSessionSnapshot?
    fun applyChatSessionSnapshot(player: Player, snapshot: PlayerChatSessionSnapshot)

    fun <T> withMessageSignatureCacheLock(player: Player, block: () -> T): T?

    fun resetPlayerChatState(player: Player, chatSession: RemoteChatSessionData)

    fun runOnChatMessageChain(player: Player, scope: CoroutineScope, block: suspend () -> Unit)

    fun sendSignedMessageWithChangedContent(
        player: Player,
        original: SignedMessage,
        boundChatType: ChatType.Bound,
        unsignedContent: Component
    )

    fun increaseNextChatIndex(player: Player): Int?

    fun createPlayerChatMessageMirrorFromAdventure(
        adventure: SignedMessage,
        unsignedContent: Component? = adventure.unsignedContent()
    ): PlayerChatMessageMirror?

    fun createAdventureChatMessageFromMirror(mirror: PlayerChatMessageMirror): SignedMessage

    fun sendPlayerChatMessage(
        receiver: Player,
        message: SignedMessage,
        boundChatType: ChatType.Bound
    )

    fun getPaperRawChatType(): ChatType

    /**
     * Loads an offline player's inventory and equipment, exposes them for mutation, and persists
     * the modified state back to disk.
     *
     * The player of the [profile] must be offline when this function is called.
     *
     * Warning: behavior is undefined, unsafe, and not supported if the player joins while this
     * operation is running.
     *
     * @param profile the offline player profile whose persisted inventory data should be edited
     * @param edit callback that receives a mutable [PlayerInventoryEdit] snapshot
     */
    suspend fun editOfflineInventory(
        profile: PlayerProfile,
        edit: (PlayerInventoryEdit) -> Unit
    )

    /**
     * Mutable snapshot used by [editOfflineInventory] to modify a player's persisted inventory
     * and equipment.
     *
     * @property items mutable main inventory contents (expected size: 36)
     * @property equipment mutable equipment view (armor/offhand/main hand)
     */
    data class PlayerInventoryEdit(
        val items: MutableList<ItemStack>,
        val equipment: EntityEquipment
    )

    /**
     * Retrieves the directory where player data is stored.
     *
     * @return a [File] representing the directory used for storing player data
     */
    fun getPlayerDataDir(): File

    /**
     * Retrieves the file system path used for storing statistical data.
     *
     * @return a [Path] object representing the location where statistical data is stored
     */
    fun getStatsDataPath(): Path

    companion object : SurfPaperNmsPlayerBridge by playerBridge {
        val INSTANCE get() = playerBridge
    }
}

@NmsUseWithCaution
private val playerBridge = requiredService<SurfPaperNmsPlayerBridge>()
