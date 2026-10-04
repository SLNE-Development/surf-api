package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.coroutine.minestomBlockingScope
import dev.slne.surf.api.minestom.server.chat.impl.signature.EXPIRED_PROFILE_PUBLIC_KEY
import dev.slne.surf.api.minestom.server.chat.impl.signature.LastSeenMessages
import dev.slne.surf.api.minestom.server.chat.impl.signature.LastSeenMessagesValidator
import dev.slne.surf.api.minestom.server.chat.impl.signature.MessageSignatureCache
import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import dev.slne.surf.api.minestom.server.chat.impl.signature.ProfilePublicKeyValidationException
import dev.slne.surf.api.minestom.server.chat.impl.signature.RemoteChatSession
import dev.slne.surf.api.minestom.server.chat.impl.signature.SignedMessageBody
import dev.slne.surf.api.minestom.server.chat.impl.signature.SignedMessageChain
import dev.slne.surf.api.minestom.server.impl.command.SignedCommandArguments
import dev.slne.surf.api.minestom.server.impl.command.signableCommandArguments
import dev.slne.surf.api.minestom.server.impl.command.signaturesCoverArguments
import dev.slne.surf.api.minestom.server.chat.SignedChatSettings
import dev.slne.surf.api.minestom.server.chat.impl.TickThrottler
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.Component.translatable
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.logger.slf4j.ComponentLogger
import net.minestom.server.MinecraftServer
import net.minestom.server.crypto.ChatSession
import net.minestom.server.crypto.SignatureValidator
import net.minestom.server.entity.Player
import net.minestom.server.message.ChatMessageType
import net.minestom.server.network.packet.client.play.ClientChatAckPacket
import net.minestom.server.network.packet.client.play.ClientChatMessagePacket
import net.minestom.server.network.packet.client.play.ClientChatSessionUpdatePacket
import net.minestom.server.network.packet.client.play.ClientSignedCommandChatPacket
import net.minestom.server.network.packet.server.play.DisguisedChatPacket
import net.minestom.server.network.packet.server.play.PlayerChatMessagePacket
import net.minestom.server.network.packet.server.play.SystemChatPacket
import java.time.Instant

internal class PlayerChatHandler(
    private val player: Player,
    private val config: SignedChatSettings,
): AutoCloseable {
    companion object {
        private val LOGGER = ComponentLogger.logger()

        private val CHAT_VALIDATION_FAILED =
            translatable("multiplayer.disconnect.chat_validation_failed")

        private val ILLEGAL_CHARACTERS =
            translatable("multiplayer.disconnect.illegal_characters")

        private val TOO_MANY_PENDING_CHATS =
            translatable("multiplayer.disconnect.too_many_pending_chats")

        private const val MAX_PENDING_CHATS = 4096

        fun isAllowedChatCharacter(char: Char): Boolean {
            val code = char.code
            return char != '§' && code >= ' '.code && code != 127
        }

        fun isChatMessageIllegal(message: String) = !message.all(::isAllowedChatCharacter)
    }

    private val lastSeenMessages = LastSeenMessagesValidator()
    private val messageSignatureCache = MessageSignatureCache.createDefault()

    private val chatSpamThrottler = TickThrottler(20, 20 * config.chatSpamThresholdSeconds)
    private val commandSpamThrottler = TickThrottler(20, 20 * config.commandSpamThresholdSeconds)

    private var nextChatIndex = 0

    private var signedMessageDecoder: SignedMessageChain.Decoder =
        SignedMessageChain.Decoder.unsigned(player.uuid) { config.enforceSecureProfile }


    @Volatile
    var chatSession: RemoteChatSession? = null
        private set

    private val chatMessageChain = ChatMessageChain(minestomBlockingScope)

    fun tick() {
        chatSpamThrottler.tick()
        commandSpamThrottler.tick()
    }

    override fun close() {
        chatMessageChain.close()
    }


    fun handleChat(packet: ClientChatMessagePacket, onMessage: suspend (PlayerChatMessage) -> Unit) {
        if (MinecraftServer.isStopping()) return

        val lastSeen = unpackAndApplyLastSeen(LastSeenMessages.Update.fromPacket(packet)) ?: return

        tryHandleChat(packet.message(), isCommand = false) {
            val signedMessage = try {
                getSignedMessage(packet, lastSeen)
            } catch (failure: SignedMessageChain.DecodeException) {
                handleMessageDecodeFailure(failure)
                return@tryHandleChat
            }

            chatMessageChain.append { broadcastChatMessage(signedMessage, onMessage) }
        }
    }

    fun handleSignedCommandChat(
        packet: ClientSignedCommandChatPacket,
        onCommand: (String) -> Unit
    ) {
        if (MinecraftServer.isStopping()) return

        val lastSeen = unpackAndApplyLastSeen(LastSeenMessages.Update.fromPacket(packet)) ?: return

        tryHandleChat(packet.message(), isCommand = true) {
            val signedArguments = getSignedArguments(packet, lastSeen) ?: return@tryHandleChat

            SignedCommandArguments.withMessages(
                signedArguments.mapValues { (_, message) -> message.adventureView() }
            ) { onCommand(packet.message()) }
            detectRateSpam(commandSpamThrottler)
        }
    }

    fun handleUnsignedCommandChat(command: String, onCommand: (String) -> Unit) {
        tryHandleChat(command, isCommand = true) {
            onCommand(command)
            detectRateSpam(commandSpamThrottler)
        }
    }

    fun handleChatAck(packet: ClientChatAckPacket) {
        synchronized(lastSeenMessages) {
            try {
                lastSeenMessages.applyOffset(packet.offset())
            } catch (failure: LastSeenMessagesValidator.ValidationException) {
                LOGGER.error(
                    "Failed to validate message acknowledgement offset from {}: {}",
                    player.username,
                    failure.message
                )
                disconnect(CHAT_VALIDATION_FAILED)
            }
        }
    }


    fun handleChatSessionUpdate(
        packet: ClientChatSessionUpdatePacket,
        onSessionReset: (RemoteChatSession) -> Unit
    ) {
        val newChatSession: ChatSession = packet.chatSession()
        val oldProfileKey = chatSession?.profilePublicKey
        val newProfileKey = newChatSession.publicKey()

        if (oldProfileKey == newProfileKey) return

        if (oldProfileKey != null && newProfileKey.expiresAt()
                .isBefore(oldProfileKey.expiresAt())
        ) {
            disconnect(EXPIRED_PROFILE_PUBLIC_KEY)
            return
        }

        try {
            val validated = RemoteChatSession.validate(
                data = newChatSession,
                profileId = player.uuid,
                serviceSignatureValidator = SignatureValidator.YGGDRASIL
            )

            resetPlayerChatState(validated, onSessionReset)
        } catch (failure: ProfilePublicKeyValidationException) {
            disconnect(failure.component)
        }
    }

    private fun resetPlayerChatState(
        session: RemoteChatSession,
        onSessionReset: (RemoteChatSession) -> Unit
    ) {
        chatSession = session
        signedMessageDecoder = session.createMessageDecoder(player.uuid)

        chatMessageChain.append { onSessionReset(session) }
    }

    private fun getSignedMessage(
        packet: ClientChatMessagePacket,
        lastSeenMessages: LastSeenMessages
    ): PlayerChatMessage {
        val body = SignedMessageBody(
            content = packet.message(),
            timeStamp = Instant.ofEpochMilli(packet.timestamp()),
            salt = packet.salt(),
            lastSeen = lastSeenMessages
        )

        return signedMessageDecoder.unpack(packet.signature(), body)
    }

    /**
     * The message the sender signed for every signable argument of [packet]'s command, keyed by node
     * name, or `null` when the command must not run.
     */
    private fun getSignedArguments(
        packet: ClientSignedCommandChatPacket,
        lastSeenMessages: LastSeenMessages
    ): Map<String, PlayerChatMessage>? {
        val entries = packet.signatures().entries()
        if (entries.isEmpty()) return emptyMap()

        val values = signableCommandArguments(player, packet.message())
        if (!signaturesCoverArguments(packet.signatures(), values)) {
            LOGGER.error(
                "Failed to match the signed arguments of {} against '{}'",
                player.username,
                packet.message()
            )
            signedMessageDecoder.setChainBroken()
            disconnect(CHAT_VALIDATION_FAILED)
            return null
        }

        val signedArguments = Object2ObjectOpenHashMap<String, PlayerChatMessage>(entries.size)
        entries.forEach { entry ->
            val body = SignedMessageBody(
                content = values.getValue(entry.name()),
                timeStamp = Instant.ofEpochMilli(packet.timestamp()),
                salt = packet.salt(),
                lastSeen = lastSeenMessages
            )

            try {
                signedArguments[entry.name()] = signedMessageDecoder.unpack(entry.signature(), body)
            } catch (failure: SignedMessageChain.DecodeException) {
                handleMessageDecodeFailure(failure)
                return null
            }
        }

        return signedArguments
    }

    private inline fun tryHandleChat(message: String, isCommand: Boolean, chatHandler: () -> Unit) {
        if (isChatMessageIllegal(message)) {
            disconnect(ILLEGAL_CHARACTERS)
        } else if (player.isRemoved ||
            (!isCommand && player.settings.chatMessageType() == ChatMessageType.NONE)
        ) {
            player.sendPacket(
                SystemChatPacket(
                    translatable("chat.disabled.options", NamedTextColor.RED),
                    false
                )
            )
        } else {
            chatHandler()
        }
    }

    private fun unpackAndApplyLastSeen(update: LastSeenMessages.Update): LastSeenMessages? {
        synchronized(lastSeenMessages) {
            try {
                return lastSeenMessages.applyUpdate(update)
            } catch (failure: LastSeenMessagesValidator.ValidationException) {
                LOGGER.error(
                    "Failed to validate message acknowledgements from {}: {}",
                    player.username,
                    failure.message
                )
                disconnect(CHAT_VALIDATION_FAILED)
                return null
            }
        }
    }

    private fun handleMessageDecodeFailure(failure: SignedMessageChain.DecodeException) {
        LOGGER.warn(
            "Failed to update secure chat state for {}: '{}'",
            player.username,
            failure.component
        )
        player.sendPacket(SystemChatPacket(failure.component.color(NamedTextColor.RED), false))
    }

    private suspend fun broadcastChatMessage(
        message: PlayerChatMessage,
        onMessage: suspend (PlayerChatMessage) -> Unit
    ) {
        val rawMessage = message.signedContent()

        if (rawMessage.isEmpty()) {
            LOGGER.warn("{} tried to send an empty message", player.username)
        } else if (player.settings.chatMessageType() == ChatMessageType.SYSTEM) {
            player.sendPacket(
                SystemChatPacket(
                    translatable("chat.cannotSend", NamedTextColor.RED),
                    false
                )
            )
        } else {
            onMessage(message)
        }

        detectRateSpam(chatSpamThrottler)
    }

    private fun detectRateSpam(throttler: TickThrottler) {
        if (!throttler.isIncrementAndUnderThreshold()) {
            disconnect(translatable("disconnect.spam"))
        }
    }

    fun sendPlayerChatMessage(message: PlayerChatMessage, chatType: BoundChatType) {
        synchronized(messageSignatureCache) {
            player.sendPacket(
                PlayerChatMessagePacket(
                    nextChatIndex++,
                    message.link.sender,
                    message.link.index,
                    message.signature,
                    message.signedBody.pack(messageSignatureCache),
                    message.unsignedContent,
                    message.filterMask,
                    chatType.id,
                    chatType.name,
                    chatType.targetName
                )
            )

            val signature = message.signature ?: return
            messageSignatureCache.push(message.signedBody, signature)

            val trackedCount = synchronized(lastSeenMessages) {
                lastSeenMessages.addPending(signature)
                lastSeenMessages.trackedMessagesCount
            }

            if (trackedCount > MAX_PENDING_CHATS) {
                disconnect(TOO_MANY_PENDING_CHATS)
            }
        }
    }

    fun sendDisguisedChatMessage(content: Component, chatType: BoundChatType) {
        player.sendPacket(
            DisguisedChatPacket(content, chatType.id, chatType.name, chatType.targetName)
        )
    }

    private fun disconnect(reason: Component) {
        player.scheduleNextTick { player.kick(reason) }
    }
}
