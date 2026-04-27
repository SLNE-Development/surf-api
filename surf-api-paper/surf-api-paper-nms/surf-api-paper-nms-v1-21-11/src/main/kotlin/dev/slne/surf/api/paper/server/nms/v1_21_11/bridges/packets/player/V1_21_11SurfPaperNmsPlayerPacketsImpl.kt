@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.player

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.packets.PacketOperation
import dev.slne.surf.api.paper.nms.bridges.packets.player.SurfPaperNmsPlayerPackets
import dev.slne.surf.api.paper.server.nms.v1_21_11.bridges.packets.V1_21_11PacketOperationImpl
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import io.papermc.paper.adventure.PaperAdventure
import io.papermc.paper.math.BlockPosition
import net.kyori.adventure.text.Component
import net.minecraft.network.chat.RemoteChatSession
import net.minecraft.network.protocol.game.*
import net.minecraft.world.entity.player.ProfilePublicKey
import org.bukkit.GameMode
import org.bukkit.event.inventory.InventoryType
import org.bukkit.inventory.ItemStack
import java.util.*

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerPacketsImpl : SurfPaperNmsPlayerPackets {

    override fun openSignEditor(
        position: BlockPosition,
        frontSide: Boolean,
    ) = V1_21_11PacketOperationImpl.simple { ClientboundOpenSignEditorPacket(position.toNms(), frontSide) }

    override fun openInventory(
        syncId: Int,
        type: InventoryType,
        title: Component,
    ) = V1_21_11PacketOperationImpl.simple {
        ClientboundOpenScreenPacket(
            syncId,
            type.toNms(),
            title.toNms()
        )
    }

    override fun setInventorySlot(
        syncId: Int,
        revision: Int,
        slot: Int,
        item: ItemStack,
    ) = V1_21_11PacketOperationImpl.simple {
        ClientboundContainerSetSlotPacket(
            syncId,
            revision,
            slot,
            item.toNms()
        )
    }

    override fun closeInventory(syncId: Int) =
        V1_21_11PacketOperationImpl.simple { ClientboundContainerClosePacket(syncId) }

    override fun createNewPlayerInfoUpdate(
        profileId: UUID,
        profile: PlayerProfile,
        listed: Boolean,
        latency: Int,
        gameMode: GameMode,
        displayName: Component?,
        showHat: Boolean,
        listOrder: Int,
        chatSession: SurfPaperNmsPlayerPackets.RemoteChatSessionData?,
    ): PacketOperation = V1_21_11PacketOperationImpl.simple {
        val entries = listOf(
            ClientboundPlayerInfoUpdatePacket.Entry(
                profileId,
                (profile as CraftPlayerProfile).buildGameProfile(),
                listed,
                latency,
                gameMode.toNms(),
                PaperAdventure.asVanilla(displayName),
                showHat,
                listOrder,
                chatSession?.let { sessionData ->
                    RemoteChatSession.Data(
                        sessionData.sessionId,
                        ProfilePublicKey.Data(
                            sessionData.expiresAt,
                            sessionData.key,
                            sessionData.keySignature
                        )
                    )
                }
            )
        )

        ClientboundPlayerInfoUpdatePacket(
            createNewPlayerInfoUpdateActions,
            entries
        )
    }

    override fun removePlayerInfoUpdate(profileIds: List<UUID>): PacketOperation = V1_21_11PacketOperationImpl.simple {
        ClientboundPlayerInfoRemovePacket(profileIds)
    }

    companion object {
        private val createNewPlayerInfoUpdateActions = EnumSet.of(
            ClientboundPlayerInfoUpdatePacket.Action.ADD_PLAYER,
            ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_GAME_MODE,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LISTED,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LATENCY,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_HAT,
            ClientboundPlayerInfoUpdatePacket.Action.UPDATE_LIST_ORDER
        )
    }
}
