package dev.slne.surf.api.paper.server.nms.v1_21_11.bridges

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.paper.command.util.idOrThrow
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge.PlayerInventoryEdit
import dev.slne.surf.api.paper.nms.bridges.data.chat.PlayerChatMessageMirror
import dev.slne.surf.api.paper.nms.bridges.data.chat.RemoteChatSessionData
import dev.slne.surf.api.paper.nms.common.dummy.DummyEntityEquipment
import dev.slne.surf.api.paper.server.nms.v1_21_11.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v1_21_11.reflection.V1_21_11NmsReflections
import io.papermc.paper.adventure.PaperAdventure
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.kyori.adventure.chat.ChatType
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minecraft.core.NonNullList
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.NbtIo
import net.minecraft.network.chat.*
import net.minecraft.network.protocol.game.ClientboundPlayerChatPacket
import net.minecraft.server.MinecraftServer
import net.minecraft.server.players.NameAndId
import net.minecraft.util.ProblemReporter
import net.minecraft.util.ProblemReporter.ScopedCollector
import net.minecraft.util.Util
import net.minecraft.world.ItemStackWithSlot
import net.minecraft.world.entity.EntityEquipment
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.npc.InventoryCarrier
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.level.storage.TagValueInput
import net.minecraft.world.level.storage.TagValueOutput
import net.minecraft.world.level.storage.ValueInput
import net.minecraft.world.level.storage.ValueOutput
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createTempFile
import kotlin.jvm.optionals.getOrNull

@NmsUseWithCaution
class V1_21_11SurfPaperNmsPlayerBridgeImpl : SurfPaperNmsPlayerBridge {
    override fun getRemoteChatSessionData(player: Player): RemoteChatSessionData? {
        val session = player.toNms().chatSession?.asData() ?: return null
        val profilePublicKey = session.profilePublicKey()

        return RemoteChatSessionData(
            sessionId = session.sessionId(),
            expiresAt = profilePublicKey.expiresAt(),
            key = profilePublicKey.key(),
            keySignature = profilePublicKey.keySignature()
        )
    }

    override fun runOnChatMessageChain(player: Player, scope: CoroutineScope, block: suspend () -> Unit) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection
        val chatMessageChain = V1_21_11NmsReflections.getChatMessageChain(connection)
        val done = CompletableFuture<Unit>()

        synchronized(chatMessageChain) {
            chatMessageChain.append {
                scope.launch {
                    try {
                        block()
                        done.complete(Unit)
                    } catch (e: Throwable) {
                        done.completeExceptionally(e)
                    }
                }
            }

            chatMessageChain.append(done) { /* no-op */ }
        }
    }

    override fun sendSignedMessageWithChangedContent(
        player: Player,
        original: SignedMessage,
        boundChatType: ChatType.Bound,
        unsignedContent: Component
    ) {
        if (original !is PlayerChatMessage.AdventureView) {
            if (original.isSystem) {
                player.sendMessage(unsignedContent, boundChatType)
            } else {
                player.sendMessage(unsignedContent, boundChatType)
            }
            return
        }
        val nmsPlayer = player.toNms()

        nmsPlayer.sendChatMessage(
            OutgoingChatMessage.create(original.playerChatMessage()),
            nmsPlayer.isTextFilteringEnabled,
            boundChatType.toNms(nmsPlayer),
            PaperAdventure.asVanilla(unsignedContent)
        )
    }

    @Suppress("USELESS_ELVIS")
    override fun increaseNextChatIndex(player: Player): Int? {
        val connection = player.toNms().connection ?: return null
        return V1_21_11NmsReflections.getAndIncreaseNextChatIndex(connection)
    }

    override fun createPlayerChatMessageMirrorFromAdventure(
        adventure: SignedMessage,
        unsignedContent: Component?
    ): PlayerChatMessageMirror? {
        if (adventure !is PlayerChatMessage.AdventureView) return null
        val nms = adventure.playerChatMessage()
        val nmsLink = nms.link
        val nmsBody = nms.signedBody()
        val nmsLastSeen = nmsBody.lastSeen()

        val link = PlayerChatMessageMirror.SignedMessageLink(nmsLink.index(), nmsLink.sender(), nmsLink.sessionId())
        val lastSeen = PlayerChatMessageMirror.SignedMessageBody.LastSeenMessages(
            nmsLastSeen.entries().map { it.bytes() }
        )
        val body = PlayerChatMessageMirror.SignedMessageBody(
            nmsBody.content(),
            nmsBody.timeStamp(),
            nmsBody.salt(),
            lastSeen
        )

        val filterMask = when (val mask = nms.filterMask()) {
            FilterMask.FULLY_FILTERED -> PlayerChatMessageMirror.FilterMask.FULLY_FILTERED
            FilterMask.PASS_THROUGH -> PlayerChatMessageMirror.FilterMask.PASS_THROUGH
            else -> PlayerChatMessageMirror.FilterMask(V1_21_11NmsReflections.getFilterMask(mask))
        }

        return PlayerChatMessageMirror(
            link = link,
            signature = nms.signature()?.bytes(),
            unsignedContent = unsignedContent,
            signedBody = body,
            filterMask = filterMask,
        )
    }

    override fun createAdventureChatMessageFromMirror(mirror: PlayerChatMessageMirror): SignedMessage {
        val mirrorLink = mirror.link
        val mirrorBody = mirror.signedBody
        val mirrorLastSeen = mirrorBody.lastSeen

        val link = SignedMessageLink(mirrorLink.index, mirrorLink.sender, mirrorLink.sessionId)
        val lastSeen = LastSeenMessages(mirrorLastSeen.entries.map(::MessageSignature))
        val body = SignedMessageBody(mirrorBody.content, mirrorBody.timestamp, mirrorBody.salt, lastSeen)

        val filterMask = when (mirror.filterMask) {
            PlayerChatMessageMirror.FilterMask.FULLY_FILTERED -> FilterMask.FULLY_FILTERED
            PlayerChatMessageMirror.FilterMask.PASS_THROUGH -> FilterMask.PASS_THROUGH
            else -> V1_21_11NmsReflections.createFilterMask(mirror.filterMask.mask)
        }

        return PlayerChatMessage(
            link,
            mirror.signature?.let(::MessageSignature),
            body,
            PaperAdventure.asVanilla(mirror.unsignedContent),
            filterMask
        ).adventureView()
    }

    @Suppress("USELESS_ELVIS")
    override fun sendPlayerChatMessage(receiver: Player, message: SignedMessage, boundChatType: ChatType.Bound) {
        require(message is PlayerChatMessage.AdventureView) { "Only native nms messages can be sent" }
        val nmsMessage = message.playerChatMessage()
        val nmsPlayer = receiver.toNms()
        val connection = nmsPlayer.connection ?: return
        val messageSignatureCache = V1_21_11NmsReflections.getMessageSignatureCache(connection)

        synchronized(messageSignatureCache) {
            connection.send(
                ClientboundPlayerChatPacket(
                    V1_21_11NmsReflections.getAndIncreaseNextChatIndex(connection),
                    nmsMessage.link().sender(),
                    nmsMessage.link().index(),
                    nmsMessage.signature(),
                    nmsMessage.signedBody().pack(messageSignatureCache),
                    nmsMessage.unsignedContent(),
                    nmsMessage.filterMask(),
                    boundChatType.toNms(nmsPlayer)
                )
            )

            val signature = nmsMessage.signature()
            if (signature != null) {
                messageSignatureCache.push(nmsMessage.signedBody(), signature)
                val lastSeenMessages = V1_21_11NmsReflections.getLastSeenMessages(connection)

                synchronized(lastSeenMessages) {
                    lastSeenMessages.addPending(signature)
                }
            }
        }
    }

    override fun getPaperRawChatType(): ChatType {
        return ChatType.chatType(PaperAdventure.asAdventureKey(V1_21_11NmsReflections.getPaperRawChatTypeKey()))
    }

    override suspend fun editOfflineInventory(
        profile: PlayerProfile,
        edit: (PlayerInventoryEdit) -> Unit
    ) {
        val uuid = profile.idOrThrow()
        require(server.getPlayer(uuid) == null) { "Player must be offline" }
        require(profile is CraftPlayerProfile) { "Only CraftPlayerProfile (paper) is supported" }

        val server = MinecraftServer.getServer()
        val nameAndId = NameAndId(profile.gameProfileUnsafe)
        val rootPathElement = ProblemReporter.PathElement { "OfflinePlayer Inventory[$nameAndId]" }
        val currentTag = loadPlayerTag(server, nameAndId)
        val inventoryEdit = buildInventoryEdit(server, rootPathElement, currentTag)

        edit(inventoryEdit)
        saveInventoryEdit(server, rootPathElement, currentTag, nameAndId, inventoryEdit)
    }

    private suspend fun loadPlayerTag(server: MinecraftServer, nameAndId: NameAndId): CompoundTag {
        val dataStorage = server.playerDataStorage
        return withContext(Dispatchers.IO) {
            dataStorage.load(nameAndId).getOrNull() ?: CompoundTag()
        }
    }

    private fun buildInventoryEdit(
        server: MinecraftServer,
        root: ProblemReporter.PathElement,
        currentTag: CompoundTag
    ): PlayerInventoryEdit = ScopedCollector(root, OFFLINE_INVENTORY_EDIT_LOGGER).use { reporter ->
        val input = TagValueInput.create(reporter, server.registryAccess(), currentTag)
        val items = loadItems(input.listOrEmpty(InventoryCarrier.TAG_INVENTORY, ItemStackWithSlot.CODEC))
        val nmsEquipment = input.read(LivingEntity.TAG_EQUIPMENT, EntityEquipment.CODEC).getOrNull()
            ?: EntityEquipment()

        PlayerInventoryEdit(items, EntityEquipmentMirror(nmsEquipment))
    }

    private suspend fun saveInventoryEdit(
        server: MinecraftServer,
        root: ProblemReporter.PathElement,
        currentTag: CompoundTag,
        nameAndId: NameAndId,
        inventoryEdit: PlayerInventoryEdit
    ) {
        ScopedCollector(root, OFFLINE_INVENTORY_EDIT_LOGGER).use { reporter ->
            val output = TagValueOutput.createWrappingWithContext(reporter, server.registryAccess(), currentTag)
            saveItems(
                inventoryEdit.items,
                output.list(InventoryCarrier.TAG_INVENTORY, ItemStackWithSlot.CODEC)
            )

            val nmsEquipment = (inventoryEdit.equipment as EntityEquipmentMirror).equipment
            if (!nmsEquipment.isEmpty) {
                output.store(LivingEntity.TAG_EQUIPMENT, EntityEquipment.CODEC, nmsEquipment)
            } else {
                output.discard(LivingEntity.TAG_EQUIPMENT)
            }

            writePlayerTag(server, nameAndId, output.buildResult())
        }
    }

    private suspend fun writePlayerTag(
        server: MinecraftServer,
        nameAndId: NameAndId,
        rootTag: CompoundTag
    ) {
        try {
            val playerDirPath = server.playerDataStorage.playerDir.toPath()
            val playerId = nameAndId.id.toString()
            val tmp = createTempFile(playerDirPath, "$playerId-", ".dat")

            withContext(Dispatchers.IO) {
                NbtIo.writeCompressed(rootTag, tmp)
                Util.safeReplaceFile(
                    playerDirPath.resolve("$playerId.dat"),
                    tmp,
                    playerDirPath.resolve("${playerId}.dat_old")
                )
            }
        } catch (e: Exception) {
            OFFLINE_INVENTORY_EDIT_LOGGER.error("Failed to save offline player inventory", e)
            throw e
        }
    }

    private fun loadItems(input: ValueInput.TypedInputList<ItemStackWithSlot>): MutableList<ItemStack> {
        val items = NonNullList.withSize(Inventory.INVENTORY_SIZE, ItemStack.empty())

        for (item in input) {
            if (item.isValidInContainer(items.size)) {
                items[item.slot()] = item.stack().asBukkitMirror()
            }
        }

        return items
    }

    private fun saveItems(items: List<ItemStack>, output: ValueOutput.TypedOutputList<ItemStackWithSlot>) {
        for ((index, stack) in items.withIndex()) {
            if (!stack.isEmpty) {
                output.add(ItemStackWithSlot(index, stack.toNms()))
            }
        }
    }

    private class EntityEquipmentMirror(val equipment: EntityEquipment) : DummyEntityEquipment() {
        override fun setItem(slot: EquipmentSlot, item: ItemStack?) {
            val nmsSlot = CraftEquipmentSlot.getNMS(slot)
            val nmsStack = CraftItemStack.asNMSCopy(item)
            equipment.set(nmsSlot, nmsStack)
        }

        override fun getItem(slot: EquipmentSlot): ItemStack {
            val nmsSlot = CraftEquipmentSlot.getNMS(slot)
            return equipment.get(nmsSlot).asBukkitMirror()
        }

        override fun clear() {
            equipment.clear()
        }
    }

    companion object {
        private val OFFLINE_INVENTORY_EDIT_LOGGER = ComponentLogger.logger("OfflinePlayer Inventory Edit")
    }
}