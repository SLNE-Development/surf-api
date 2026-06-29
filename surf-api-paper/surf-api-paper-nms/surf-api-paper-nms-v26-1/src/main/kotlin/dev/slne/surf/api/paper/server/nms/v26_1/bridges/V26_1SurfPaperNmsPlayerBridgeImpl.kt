package dev.slne.surf.api.paper.server.nms.v26_1.bridges

import com.destroystokyo.paper.profile.CraftPlayerProfile
import com.destroystokyo.paper.profile.PlayerProfile
import dev.slne.surf.api.paper.command.util.idOrThrow
import dev.slne.surf.api.paper.extensions.server
import dev.slne.surf.api.paper.nms.NmsUseWithCaution
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge
import dev.slne.surf.api.paper.nms.bridges.SurfPaperNmsPlayerBridge.PlayerInventoryEdit
import dev.slne.surf.api.paper.nms.bridges.data.chat.*
import dev.slne.surf.api.paper.nms.common.dummy.DummyEntityEquipment
import dev.slne.surf.api.paper.server.nms.v26_1.extensions.toNms
import dev.slne.surf.api.paper.server.nms.v26_1.reflection.V26_1NmsReflections
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
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket
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
import net.minecraft.world.entity.player.ProfilePublicKey
import net.minecraft.world.level.storage.*
import org.bukkit.craftbukkit.CraftEquipmentSlot
import org.bukkit.craftbukkit.inventory.CraftItemStack
import org.bukkit.entity.Player
import org.bukkit.inventory.EquipmentSlot
import org.bukkit.inventory.ItemStack
import java.io.File
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletableFuture
import kotlin.io.path.createTempFile
import kotlin.jvm.optionals.getOrNull

@NmsUseWithCaution
@Suppress("ClassName")
class V26_1SurfPaperNmsPlayerBridgeImpl : SurfPaperNmsPlayerBridge {

    override fun removeAllTrackedEntities(player: Player, swallowExceptions: Boolean) {
        val nmsPlayer = player.toNms()

        val distance = player.viewDistance.toDouble()
        player.getNearbyEntities(distance, distance, distance).forEach { entity ->
            try {
                entity.toNms().`moonrise$getTrackedEntity`().serverEntity.removePairing(nmsPlayer)
            } catch (e: Throwable) {
                if (!swallowExceptions) {
                    throw e
                }
            }
        }
    }

    override fun removeAllTrackedPlayers(player: Player, swallowExceptions: Boolean) {
        val nmsPlayer = player.toNms()
        val trackedEntity = nmsPlayer.`moonrise$getTrackedEntity`()

        for (otherPlayer in MinecraftServer.getServer().playerList.players) {
            if (otherPlayer.uuid == nmsPlayer.uuid) continue
            try {
                trackedEntity.serverEntity.removePairing(otherPlayer)
            } catch (e: Throwable) {
                if (!swallowExceptions) {
                    throw e
                }
            }
        }
    }

    @Suppress("USELESS_ELVIS")
    override fun getRemoteChatSessionData(player: Player): RemoteChatSessionData? {
        val connection = player.toNms().connection ?: return null
        val session = V26_1NmsReflections.getRemoteChatSession(connection) ?: return null
        val profilePublicKey = session.profilePublicKey()

        return RemoteChatSessionData(
            sessionId = session.sessionId(),
            expiresAt = profilePublicKey.data().expiresAt(),
            key = profilePublicKey.data().key(),
            keySignature = profilePublicKey.data().keySignature()
        )
    }

    @Suppress("USELESS_ELVIS")
    override fun <T> withMessageSignatureCacheLock(player: Player, block: () -> T): T? {
        val connection = player.toNms().connection ?: return null
        val cache = V26_1NmsReflections.getMessageSignatureCache(connection)
        return synchronized(cache) { block() }
    }

    @Suppress("USELESS_ELVIS")
    override fun createChatSessionSnapshot(player: Player): PlayerChatSessionSnapshot? {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection ?: return null

        val lastSeenMessages = V26_1NmsReflections.getLastSeenMessages(connection)
        val messageSignatureCache = V26_1NmsReflections.getMessageSignatureCache(connection)

        return synchronized(lastSeenMessages) {
            synchronized(messageSignatureCache) {
                val nextChatIndex = V26_1NmsReflections.getNextChatIndex(connection)
                val chatSession = V26_1NmsReflections.getRemoteChatSession(connection) ?: return null

                val lastSeenMessagesMirror = LastSeenMessagesValidatorMirror(
                    lastSeenCount = V26_1NmsReflections.getLastSeenCountFromMessageValidator(lastSeenMessages),
                    trackedMessages = V26_1NmsReflections.getTrackedMessagesFromMessageValidator(lastSeenMessages)
                        .map { entry ->
                            entry?.let {
                                LastSeenMessagesValidatorMirror.LastSeenTrackedEntry(
                                    it.signature().bytes(),
                                    it.pending()
                                )
                            }
                        },
                    lastPendingMessage = V26_1NmsReflections.getLastPendingMessageFromMessageValidator(lastSeenMessages)
                        ?.bytes()
                )

                val messageSignatureCacheEntries =
                    V26_1NmsReflections.getEntriesFromMessageSignatureCache(messageSignatureCache)
                val messageSignatureCacheMirror = Array(messageSignatureCacheEntries.size) { i ->
                    messageSignatureCacheEntries[i]?.bytes()
                }

                val chatSessionData = chatSession.asData()
                val chatSessionMirror = RemoteChatSessionData(
                    chatSessionData.sessionId,
                    chatSessionData.profilePublicKey.expiresAt,
                    chatSessionData.profilePublicKey.key,
                    chatSessionData.profilePublicKey.keySignature
                )

                val messageDecoder = V26_1NmsReflections.getSignedMessageDecoder(connection)
                val signedMessageChain = messageDecoder.signedMessageChain()

                val chatChainMirror = IncomingChatChainMirror(
                    nextLinkIndex = V26_1NmsReflections.getNextLinkFromSignedMessageChain(signedMessageChain)?.index,
                    lastTimeStamp = V26_1NmsReflections.getLastTimeStampFromSignedMessageChain(signedMessageChain)
                )

                PlayerChatSessionSnapshot(
                    nextChatIndex = nextChatIndex,
                    chatSession = chatSessionMirror,
                    lastSeenMessages = lastSeenMessagesMirror,
                    messageSignatureCache = messageSignatureCacheMirror,
                    incomingChatChain = chatChainMirror
                )
            }
        }
    }

    @Suppress("USELESS_ELVIS")
    override fun applyChatSessionSnapshot(player: Player, snapshot: PlayerChatSessionSnapshot) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection ?: return
        val chatSessionMirror = snapshot.chatSession ?: return

        val profileKeySignatureValidator = MinecraftServer.getServer().services().profileKeySignatureValidator()
        if (profileKeySignatureValidator == null) {
            CHAT_LOGGER.warn(
                "Ignoring chat session from {} due to missing Services public key",
                nmsPlayer.gameProfile.name
            )
            return
        }

        val newChatSession = RemoteChatSession.Data(
            chatSessionMirror.sessionId,
            ProfilePublicKey.Data(
                chatSessionMirror.expiresAt,
                chatSessionMirror.key,
                chatSessionMirror.keySignature
            )
        )

        val chatSession = newChatSession.validate(nmsPlayer.gameProfile, profileKeySignatureValidator)

        val lastSeenMessages = V26_1NmsReflections.getLastSeenMessages(connection)
        val messageSignatureCache = V26_1NmsReflections.getMessageSignatureCache(connection)

        synchronized(lastSeenMessages) {
            synchronized(messageSignatureCache) {
                V26_1NmsReflections.setNextChatIndex(connection, snapshot.nextChatIndex)

                val lastSeenCount = V26_1NmsReflections.getLastSeenCountFromMessageValidator(lastSeenMessages)
                val trackedMessages = V26_1NmsReflections.getTrackedMessagesFromMessageValidator(lastSeenMessages)

                val lastSeenMessagesMirror = snapshot.lastSeenMessages
                val lastSeenCountMirror = lastSeenMessagesMirror.lastSeenCount
                val trackedMessagesMirror = lastSeenMessagesMirror.trackedMessages
                val lastPendingMessageMirror = lastSeenMessagesMirror.lastPendingMessage

                if (lastSeenCount != lastSeenCountMirror) {
                    CHAT_LOGGER.warn(
                        "Chat session from {} has lastSeenCount mismatch: expected={}, actual={}",
                        nmsPlayer.gameProfile.name, lastSeenCountMirror, lastSeenCount
                    )
                }

                trackedMessages.clear()
                for (entry in trackedMessagesMirror) {
                    if (entry == null) {
                        trackedMessages.add(null)
                    } else {
                        trackedMessages.add(
                            LastSeenTrackedEntry(MessageSignature(entry.signature), entry.pending)
                        )
                    }
                }

                val lastPendingMessage = lastPendingMessageMirror?.let { MessageSignature(it) }
                V26_1NmsReflections.setLastPendingMessageFromMessageValidator(lastSeenMessages, lastPendingMessage)

                val messageSignatureCacheEntries =
                    V26_1NmsReflections.getEntriesFromMessageSignatureCache(messageSignatureCache)
                val messageSignatureCacheMirror = snapshot.messageSignatureCache

                if (messageSignatureCacheEntries.size != messageSignatureCacheMirror.size) {
                    CHAT_LOGGER.warn(
                        "Chat session from {} has messageSignatureCache size mismatch: expected={}, actual={}",
                        nmsPlayer.gameProfile.name, messageSignatureCacheMirror.size, messageSignatureCacheEntries.size
                    )
                }

                for (i in messageSignatureCacheEntries.indices) {
                    messageSignatureCacheEntries[i] =
                        if (i >= messageSignatureCacheMirror.size) null
                        else messageSignatureCacheMirror[i]?.let { MessageSignature(it) }
                }
            }
        }

        V26_1NmsReflections.setRemoteChatSession(connection, chatSession)
        V26_1NmsReflections.setHasLoggedExpiry(connection, false)

        val messageChain = SignedMessageChain(nmsPlayer.uuid, chatSession.sessionId)
        V26_1NmsReflections.setLastTimeStampFromSignedMessageChain(
            messageChain,
            snapshot.incomingChatChain.lastTimeStamp
        )
        V26_1NmsReflections.setNextLinkFromSignedMessageChain(
            messageChain,
            snapshot.incomingChatChain.nextLinkIndex?.let { index ->
                SignedMessageLink(index, nmsPlayer.uuid, chatSession.sessionId)
            }
        )

        val signedMessageDecoder = messageChain.decoder(chatSession.profilePublicKey)
        V26_1NmsReflections.setSignedMessageDecoder(connection, signedMessageDecoder)

        val chatChain = V26_1NmsReflections.getChatMessageChain(connection)
        chatChain.append {
            nmsPlayer.setChatSession(chatSession)
            MinecraftServer.getServer().playerList.broadcastAll(
                ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT),
                    listOf(nmsPlayer)
                ),
                nmsPlayer
            )
        }
    }

    private fun SignedMessageChain.Decoder.signedMessageChain(): SignedMessageChain {
        try {
            val field = javaClass.getDeclaredField("this$0")
            field.trySetAccessible()
            return field.get(this) as SignedMessageChain
        } catch (e: Throwable) {
            throw RuntimeException("Failed to access signedMessageChain from decoder $this", e)
        }
    }

    @Suppress("USELESS_ELVIS")
    override fun resetPlayerChatState(player: Player, chatSession: RemoteChatSessionData) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection ?: return

        val newChatSessionData = RemoteChatSession.Data(
            chatSession.sessionId,
            ProfilePublicKey.Data(
                chatSession.expiresAt,
                chatSession.key,
                chatSession.keySignature
            )
        )

        val signatureValidator = MinecraftServer.getServer().services().profileKeySignatureValidator()
        if (signatureValidator == null) {
            CHAT_LOGGER.warn("Ignoring chat session from {} due to missing Services public key", player.name)
            return
        }

        val newChatSession = newChatSessionData.validate(
            nmsPlayer.gameProfile,
            signatureValidator
        )

        V26_1NmsReflections.resetPlayerChatState(connection, newChatSession)
    }

    override fun runOnChatMessageChain(player: Player, scope: CoroutineScope, block: suspend () -> Unit) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection
        val chatMessageChain = V26_1NmsReflections.getChatMessageChain(connection)
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
        return V26_1NmsReflections.getAndIncreaseNextChatIndex(connection)
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
            else -> PlayerChatMessageMirror.FilterMask(V26_1NmsReflections.getFilterMask(mask))
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
            else -> V26_1NmsReflections.createFilterMask(mirror.filterMask.mask)
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
        val messageSignatureCache = V26_1NmsReflections.getMessageSignatureCache(connection)

        synchronized(messageSignatureCache) {
            connection.send(
                ClientboundPlayerChatPacket(
                    V26_1NmsReflections.getAndIncreaseNextChatIndex(connection),
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
                val lastSeenMessages = V26_1NmsReflections.getLastSeenMessages(connection)

                synchronized(lastSeenMessages) {
                    lastSeenMessages.addPending(signature)
                }
            }
        }
    }

    override fun getPaperRawChatType(): ChatType {
        return ChatType.chatType(PaperAdventure.asAdventureKey(V26_1NmsReflections.getPaperRawChatTypeKey()))
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

    override fun getPlayerDataDir(): File {
        return MinecraftServer.getServer().playerDataStorage.playerDir
    }

    override fun getStatsDataPath(): Path {
        return MinecraftServer.getServer().getWorldPath(LevelResource.PLAYER_STATS_DIR)
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
        private val CHAT_LOGGER = ComponentLogger.logger("SurfPaperNmsPlayerBridge Chat")
    }
}