package dev.slne.surf.api.paper.nms.bridges

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge.Companion.editOfflineInventory
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatMessageMirror
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import kotlinx.coroutines.CoroutineScope
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import org.bukkit.inventory.EntityEquipment
import org.bukkit.inventory.ItemStack
import org.jetbrains.annotations.ApiStatus

@NmsUseWithCaution
@ApiStatus.NonExtendable
interface SurfPaperNmsPlayerBridge {

    fun getRemoteChatSessionData(player: Player): RemoteChatSessionData?

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
     * The [player] must be offline when this function is called.
     *
     * Warning: behavior is undefined, unsafe, and not supported if the player joins while this
     * operation is running.
     *
     * @param player the offline player whose persisted inventory data should be edited
     * @param edit callback that receives a mutable [PlayerInventoryEdit] snapshot
     */
    suspend fun editOfflineInventory(
        player: OfflinePlayer,
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

    companion object : SurfPaperNmsPlayerBridge by playerBridge {
        val INSTANCE get() = playerBridge
    }
}

@NmsUseWithCaution
private val playerBridge = requiredService<SurfPaperNmsPlayerBridge>()