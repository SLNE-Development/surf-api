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
import net.minecraft.server.level.ServerPlayer
import net.minecraft.server.network.ServerGamePacketListenerImpl
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
class V26_1SurfPaperNmsPlayerBridgeImpl : SurfPaperNmsPlayerBridge {


    private fun chatDebug(message: () -> String) {
        CHAT_TRANSFER_LOGGER.info("[chat-xfer] {}", message())
    }

    private fun ByteArray?.shortSig(): String {
        if (this == null) return "null"

        return take(6).joinToString("") {
            "%02x".format(it.toInt() and 0xff)
        }
    }

    private fun LastSeenMessagesValidator.debugSummary(): String {
        val tracked =
            V26_1NmsReflections.getTrackedMessagesFromMessageValidator(this)

        val nonNull =
            tracked.count { it != null }

        val pending =
            tracked.count { it?.pending() == true }

        val lastPending =
            V26_1NmsReflections.getLastPendingMessageFromMessageValidator(this)

        return "lastSeenCount=${V26_1NmsReflections.getLastSeenCountFromMessageValidator(this)} " +
                "trackedSize=${tracked.size} trackedNonNull=$nonNull trackedPending=$pending " +
                "lastPending=${lastPending?.bytes?.shortSig()}"
    }

    private fun MessageSignatureCache.debugSummary(): String {
        val entries =
            V26_1NmsReflections.getEntriesFromMessageSignatureCache(this)

        val nonNull =
            entries.count { it != null }

        return "cacheSize=${entries.size} cacheNonNull=$nonNull first=${entries.firstOrNull()?.bytes()?.shortSig()}"
    }

    private fun ServerGamePacketListenerImpl.debugRemoteSessionId(): UUID? {
        return V26_1NmsReflections.getRemoteChatSession(this)?.sessionId()
    }

    private fun ServerGamePacketListenerImpl.debugDecoderLink(): SignedMessageLink? {
        val decoder =
            V26_1NmsReflections.getSignedMessageDecoder(this)

        val chain =
            decoder.signedMessageChainOrNull()

        return chain?.let {
            V26_1NmsReflections.getNextLinkFromSignedMessageChain(it)
        }
    }


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

    override fun createChatSessionSnapshot(player: Player): PlayerChatSessionSnapshot {
        val nmsPlayer = player.toNms()
        val connection = requireNotNull(nmsPlayer.connection) {
            "Player connection is not available"
        }

        val lastSeenMessages = V26_1NmsReflections.getLastSeenMessages(connection)
        val messageSignatureCache = V26_1NmsReflections.getMessageSignatureCache(connection)

        synchronized(messageSignatureCache) {
            synchronized(lastSeenMessages) {
                val nextChatIndex = V26_1NmsReflections.getNextChatIndex(connection)
                val chatSessionData = V26_1NmsReflections.getRemoteChatSession(connection)?.asData()

                val cacheEntries =
                    V26_1NmsReflections.getEntriesFromMessageSignatureCache(messageSignatureCache)

                val decoder = V26_1NmsReflections.getSignedMessageDecoder(connection)
                val signedMessageChain = decoder.signedMessageChainOrNull()
                val nextLink = signedMessageChain?.let {
                    V26_1NmsReflections.getNextLinkFromSignedMessageChain(it)
                }

                val incomingChatChain = if (signedMessageChain != null && nextLink != null) {
                    IncomingChatChainMirror(
                        nextLinkIndex = nextLink.index(),
                        lastTimeStamp = V26_1NmsReflections.getLastTimeStampFromSignedMessageChain(signedMessageChain)
                    )
                } else {
                    null
                }

                chatDebug {
                    "CREATE_SNAPSHOT uuid=${player.uniqueId} name=${player.name} " +
                            "connectionSession=${V26_1NmsReflections.getRemoteChatSession(connection)?.sessionId()} " +
                            "playerSession=${nmsPlayer.chatSession?.asData()?.sessionId()} " +
                            "nextChatIndex=$nextChatIndex " +
                            "decoderNextIndex=${nextLink?.index()} decoderNextSession=${nextLink?.sessionId()} " +
                            "incomingNextLink=${incomingChatChain?.nextLinkIndex} " +
                            "lastSeen=${lastSeenMessages.debugSummary()} " +
                            "cache=${messageSignatureCache.debugSummary()}"
                }

                return PlayerChatSessionSnapshot(
                    nextChatIndex = nextChatIndex,
                    chatSession = chatSessionData?.let {
                        RemoteChatSessionData(
                            sessionId = it.sessionId(),
                            expiresAt = it.profilePublicKey().expiresAt(),
                            key = it.profilePublicKey().key(),
                            keySignature = it.profilePublicKey().keySignature()
                        )
                    },
                    lastSeenMessages = LastSeenMessagesValidatorMirror(
                        V26_1NmsReflections.getLastSeenCountFromMessageValidator(lastSeenMessages),
                        V26_1NmsReflections.getTrackedMessagesFromMessageValidator(lastSeenMessages)
                            .map { entry ->
                                entry?.let {
                                    LastSeenMessagesValidatorMirror.LastSeenTrackedEntry(
                                        it.signature().bytes(),
                                        it.pending()
                                    )
                                }
                            },
                        V26_1NmsReflections
                            .getLastPendingMessageFromMessageValidator(lastSeenMessages)
                            ?.bytes
                    ),
                    messageSignatureCache = Array(cacheEntries.size) { index ->
                        cacheEntries[index]?.bytes()
                    },
                    incomingChatChain = incomingChatChain
                )
            }
        }
    }

    override fun applyChatSessionSnapshot(player: Player, snapshot: PlayerChatSessionSnapshot) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection ?: return

        val sessionData = snapshot.chatSession
        if (sessionData == null) {
            chatDebug {
                "APPLY_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_snapshot_session"
            }
            return
        }

        val currentRemoteChatSession = V26_1NmsReflections.getRemoteChatSession(connection)

        chatDebug {
            "APPLY_START uuid=${player.uniqueId} name=${player.name} " +
                    "snapshotSession=${sessionData.sessionId} " +
                    "currentConnectionSession=${currentRemoteChatSession?.sessionId()} " +
                    "currentPlayerSession=${nmsPlayer.chatSession?.asData()?.sessionId()} " +
                    "snapshotNextChatIndex=${snapshot.nextChatIndex} " +
                    "snapshotIncomingNextLink=${snapshot.incomingChatChain?.nextLinkIndex}"
        }

        val remoteChatSession = when {
            currentRemoteChatSession != null &&
                    currentRemoteChatSession.sessionId() == sessionData.sessionId -> {
                chatDebug {
                    "APPLY_SESSION_USE_CURRENT_MATCH uuid=${player.uniqueId} name=${player.name} " +
                            "session=${currentRemoteChatSession.sessionId()}"
                }

                nmsPlayer.setChatSession(currentRemoteChatSession)

                MinecraftServer.getServer().playerList.broadcastAll(
                    ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT),
                        listOf(nmsPlayer)
                    ),
                    nmsPlayer
                )

                currentRemoteChatSession
            }

            currentRemoteChatSession != null -> {
                chatDebug {
                    "APPLY_SKIP uuid=${player.uniqueId} name=${player.name} reason=session_mismatch " +
                            "currentSession=${currentRemoteChatSession.sessionId()} " +
                            "snapshotSession=${sessionData.sessionId}"
                }

                // Extrem wichtig:
                // Bei neuer Vanilla-Session den alten Snapshot komplett verwerfen.
                // Kein lastSeen, kein cache, kein nextChatIndex, kein decoder patch.
                return
            }

            else -> {
                val profileKeyValidator =
                    MinecraftServer.getServer().services().profileKeySignatureValidator()
                        ?: run {
                            chatDebug {
                                "APPLY_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_profile_key_validator"
                            }
                            return
                        }

                val remoteChatSessionData = RemoteChatSession.Data(
                    sessionData.sessionId,
                    ProfilePublicKey.Data(
                        sessionData.expiresAt,
                        sessionData.key,
                        sessionData.keySignature
                    )
                )

                val createdRemoteChatSession = remoteChatSessionData.validate(
                    nmsPlayer.gameProfile,
                    profileKeyValidator
                )

                V26_1NmsReflections.setRemoteChatSession(
                    connection,
                    createdRemoteChatSession
                )

                V26_1NmsReflections.setHasLoggedExpiry(
                    connection,
                    false
                )

                V26_1NmsReflections.setSignedMessageDecoder(
                    connection,
                    createdRemoteChatSession.createMessageDecoder(player.uniqueId)
                )

                nmsPlayer.setChatSession(createdRemoteChatSession)

                MinecraftServer.getServer().playerList.broadcastAll(
                    ClientboundPlayerInfoUpdatePacket(
                        EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT),
                        listOf(nmsPlayer)
                    ),
                    nmsPlayer
                )

                chatDebug {
                    "APPLY_SESSION_CREATED uuid=${player.uniqueId} name=${player.name} " +
                            "session=${createdRemoteChatSession.sessionId()} " +
                            "decoderIndex=${connection.debugDecoderLink()?.index()} " +
                            "decoderSession=${connection.debugDecoderLink()?.sessionId()}"
                }

                createdRemoteChatSession
            }
        }

        val lastSeenMessages =
            V26_1NmsReflections.getLastSeenMessages(connection)

        val messageSignatureCache =
            V26_1NmsReflections.getMessageSignatureCache(connection)

        synchronized(messageSignatureCache) {
            synchronized(lastSeenMessages) {
                V26_1NmsReflections.setNextChatIndex(
                    connection,
                    snapshot.nextChatIndex
                )

                val messages =
                    V26_1NmsReflections.getTrackedMessagesFromMessageValidator(lastSeenMessages)

                messages.clear()

                for (entry in snapshot.lastSeenMessages.trackedMessages) {
                    if (entry == null) {
                        messages.add(null)
                    } else {
                        messages.add(
                            LastSeenTrackedEntry(
                                MessageSignature(entry.signature),

                                // Erstmal so lassen, weil das den
                                // "previously acknowledged" Fehler verhindert.
                                true
                            )
                        )
                    }
                }

                V26_1NmsReflections.setLastPendingMessageFromMessageValidator(
                    lastSeenMessages,
                    snapshot.lastSeenMessages.lastPendingMessage?.let(::MessageSignature)
                )

                chatDebug {
                    "APPLY_LAST_SEEN_DONE uuid=${player.uniqueId} name=${player.name} " +
                            "afterLastSeen=${lastSeenMessages.debugSummary()}"
                }
            }

            val incomingChatChain = snapshot.incomingChatChain

            if (incomingChatChain != null) {
                val decoder =
                    V26_1NmsReflections.getSignedMessageDecoder(connection)

                val signedMessageChain =
                    decoder.signedMessageChainOrNull()

                if (signedMessageChain != null) {
                    val beforeLink =
                        V26_1NmsReflections.getNextLinkFromSignedMessageChain(signedMessageChain)

                    chatDebug {
                        "APPLY_CHAIN_PATCH_START uuid=${player.uniqueId} name=${player.name} " +
                                "beforeIndex=${beforeLink?.index()} beforeSession=${beforeLink?.sessionId()} " +
                                "targetIndex=${incomingChatChain.nextLinkIndex} " +
                                "targetSession=${remoteChatSession.sessionId()}"
                    }

                    V26_1NmsReflections.setNextLinkFromSignedMessageChain(
                        signedMessageChain,
                        SignedMessageLink(
                            incomingChatChain.nextLinkIndex,
                            player.uniqueId,
                            remoteChatSession.sessionId()
                        )
                    )

                    V26_1NmsReflections.setLastTimeStampFromSignedMessageChain(
                        signedMessageChain,
                        incomingChatChain.lastTimeStamp
                    )

                    val afterLink =
                        V26_1NmsReflections.getNextLinkFromSignedMessageChain(signedMessageChain)

                    chatDebug {
                        "APPLY_CHAIN_PATCH_DONE uuid=${player.uniqueId} name=${player.name} " +
                                "afterIndex=${afterLink?.index()} afterSession=${afterLink?.sessionId()} " +
                                "lastTimeStamp=${incomingChatChain.lastTimeStamp}"
                    }
                } else {
                    chatDebug {
                        "APPLY_CHAIN_PATCH_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_signed_message_chain"
                    }
                }
            } else {
                chatDebug {
                    "APPLY_CHAIN_PATCH_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_incoming_chain"
                }
            }

            val messageSignatureCacheQueue = ArrayDeque<MessageSignature>()

            for (signatureBytes in snapshot.messageSignatureCache.reversed()) {
                if (signatureBytes != null) {
                    messageSignatureCacheQueue.add(MessageSignature(signatureBytes))
                }
            }

            chatDebug {
                "APPLY_CACHE_PUSH_START uuid=${player.uniqueId} name=${player.name} " +
                        "queueSize=${messageSignatureCacheQueue.size} " +
                        "beforeCache=${messageSignatureCache.debugSummary()}"
            }

            V26_1NmsReflections.pushMessageSignatureCache(
                messageSignatureCache,
                messageSignatureCacheQueue
            )

            chatDebug {
                "APPLY_CACHE_PUSH_DONE uuid=${player.uniqueId} name=${player.name} " +
                        "afterCache=${messageSignatureCache.debugSummary()} " +
                        "finalDecoderIndex=${connection.debugDecoderLink()?.index()} " +
                        "finalDecoderSession=${connection.debugDecoderLink()?.sessionId()} " +
                        "finalConnectionSession=${connection.debugRemoteSessionId()}"
            }
        }
    }

    override fun debugChatSessionState(player: Player, label: String) {
        val nmsPlayer = player.toNms()
        val connection = nmsPlayer.connection ?: return

        val lastSeenMessages =
            V26_1NmsReflections.getLastSeenMessages(connection)

        val messageSignatureCache =
            V26_1NmsReflections.getMessageSignatureCache(connection)

        val decoderLink =
            connection.debugDecoderLink()

        chatDebug {
            "STATE label=$label uuid=${player.uniqueId} name=${player.name} " +
                    "connectionSession=${connection.debugRemoteSessionId()} " +
                    "playerSession=${nmsPlayer.chatSession?.asData()?.sessionId()} " +
                    "decoderIndex=${decoderLink?.index()} " +
                    "decoderSession=${decoderLink?.sessionId()} " +
                    "nextChatIndex=${V26_1NmsReflections.getNextChatIndex(connection)} " +
                    "lastSeen=${lastSeenMessages.debugSummary()} " +
                    "cache=${messageSignatureCache.debugSummary()}"
        }
    }

    private fun restoreOrGetRemoteChatSession(
        nmsPlayer: ServerPlayer,
        connection: ServerGamePacketListenerImpl,
        player: Player,
        sessionData: RemoteChatSessionData?
    ): RemoteChatSession? {
        val currentRemoteChatSession =
            V26_1NmsReflections.getRemoteChatSession(connection)

        chatDebug {
            "RESTORE_REMOTE_SESSION_START uuid=${player.uniqueId} name=${player.name} " +
                    "currentConnectionSession=${currentRemoteChatSession?.sessionId()} " +
                    "currentPlayerSession=${nmsPlayer.chatSession?.asData()?.sessionId()} " +
                    "snapshotSession=${sessionData?.sessionId}"
        }

        if (currentRemoteChatSession != null) {
            nmsPlayer.setChatSession(currentRemoteChatSession)

            MinecraftServer.getServer().playerList.broadcastAll(
                ClientboundPlayerInfoUpdatePacket(
                    EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT),
                    listOf(nmsPlayer)
                ),
                nmsPlayer
            )

            chatDebug {
                "RESTORE_REMOTE_SESSION_USE_CURRENT uuid=${player.uniqueId} name=${player.name} " +
                        "session=${currentRemoteChatSession.sessionId()}"
            }

            return currentRemoteChatSession
        }

        if (sessionData == null) {
            chatDebug {
                "RESTORE_REMOTE_SESSION_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_snapshot_session"
            }

            return null
        }

        val profileKeyValidator =
            MinecraftServer.getServer().services().profileKeySignatureValidator()
                ?: run {
                    chatDebug {
                        "RESTORE_REMOTE_SESSION_SKIP uuid=${player.uniqueId} name=${player.name} reason=no_profile_key_validator"
                    }

                    return null
                }

        val remoteChatSessionData = RemoteChatSession.Data(
            sessionData.sessionId,
            ProfilePublicKey.Data(
                sessionData.expiresAt,
                sessionData.key,
                sessionData.keySignature
            )
        )

        val remoteChatSession = remoteChatSessionData.validate(
            nmsPlayer.gameProfile,
            profileKeyValidator
        )

        V26_1NmsReflections.setRemoteChatSession(
            connection,
            remoteChatSession
        )

        V26_1NmsReflections.setHasLoggedExpiry(
            connection,
            false
        )

        V26_1NmsReflections.setSignedMessageDecoder(
            connection,
            remoteChatSession.createMessageDecoder(player.uniqueId)
        )

        nmsPlayer.setChatSession(remoteChatSession)

        MinecraftServer.getServer().playerList.broadcastAll(
            ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.INITIALIZE_CHAT),
                listOf(nmsPlayer)
            ),
            nmsPlayer
        )

        chatDebug {
            "RESTORE_REMOTE_SESSION_CREATED uuid=${player.uniqueId} name=${player.name} " +
                    "session=${remoteChatSession.sessionId()} decoderIndex=${connection.debugDecoderLink()?.index()} " +
                    "decoderSession=${connection.debugDecoderLink()?.sessionId()}"
        }

        return remoteChatSession
    }

    private fun SignedMessageChain.Decoder.signedMessageChainOrNull(): SignedMessageChain? {
        return runCatching {
            val field = javaClass.getDeclaredField("this$0")
            field.isAccessible = true
            field.get(this) as? SignedMessageChain
        }.getOrNull()
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
        private val CHAT_TRANSFER_LOGGER = ComponentLogger.logger("ChatSessionTransfer")
    }
}