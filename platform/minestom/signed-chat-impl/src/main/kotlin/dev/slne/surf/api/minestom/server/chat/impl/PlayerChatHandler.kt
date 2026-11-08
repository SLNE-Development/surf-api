package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.coroutine.minestomBlockingScope
import dev.slne.surf.api.minestom.server.chat.SignedChatSettings
import dev.slne.surf.api.minestom.server.chat.impl.PlayerChatHandler.Companion.MAX_PENDING_CHATS
import dev.slne.surf.api.minestom.server.chat.impl.signature.*
import dev.slne.surf.api.minestom.server.impl.command.SignedCommandArguments
import dev.slne.surf.api.minestom.server.impl.command.signableCommandArguments
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

/**
 * Validates and sends the chat traffic of one player, following vanilla: last-seen
 * acknowledgements, the signature chain, spam throttling and the cache of signatures sent to the
 * client.
 *
 * Validation failures kick the player on the next tick. Chat messages are broadcast through a
 * per-player chain, so they are processed one after another in arrival order. [tick] must be called
 * once per server tick. [close] stops the chain; queued messages are dropped.
 */
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

    /**
     * Index of the next chat packet sent to this player; guarded by [messageSignatureCache].
     */
    private var nextChatIndex = 0

    private var signedMessageDecoder: SignedMessageChain.Decoder =
        SignedMessageChain.Decoder.unsigned(player.uuid) { config.enforceSecureProfile }


    /**
     * The validated chat session of the player, or `null` until the client has sent one.
     */
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


    /**
     * Validates a chat message and queues [onMessage] on the message chain.
     *
     * The last-seen update and signature are validated immediately in packet order; only the
     * broadcast runs on the chain. Does nothing while the server stops.
     */
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

    /**
     * Validates the signed arguments of a command and runs [onCommand] with them available through
     * [SignedCommandArguments]. Does nothing while the server stops.
     *
     * A command whose arguments fail validation is not run; the failure is reported to the player.
     * Every handled command counts towards the command spam threshold.
     */
    fun handleSignedCommandChat(
        packet: ClientSignedCommandChatPacket,
        onCommand: (String) -> Unit
    ) {
        if (MinecraftServer.isStopping()) return

        val lastSeen = unpackAndApplyLastSeen(LastSeenMessages.Update.fromPacket(packet)) ?: return

        tryHandleChat(packet.message(), isCommand = true) {
            val signedArguments = try {
                getSignedArguments(packet, lastSeen)
            } catch (failure: SignedMessageChain.DecodeException) {
                handleMessageDecodeFailure(failure)
                null
            }

            if (signedArguments != null) {
                SignedCommandArguments.withMessages(
                    signedArguments.mapValues { (_, message) -> message.adventureView() }
                ) { onCommand(packet.message()) }
            }

            detectRateSpam(commandSpamThrottler)
        }
    }

    /**
     * Runs [command] received without signatures.
     *
     * With [SignedChatSettings.enforceSecureProfile], a command that has signable arguments is
     * rejected instead.
     */
    fun handleUnsignedCommandChat(command: String, onCommand: (String) -> Unit) {
        tryHandleChat(command, isCommand = true) {
            if (config.enforceSecureProfile && signableCommandArguments(player, command).isNotEmpty()) {
                LOGGER.warn(
                    "Received unsigned command packet from {}, but the command requires signable arguments: {}",
                    player.username,
                    command
                )
                player.sendPacket(
                    SystemChatPacket(
                        translatable("chat.disabled.invalid_command_signature", NamedTextColor.RED),
                        false
                    )
                )
            } else {
                onCommand(command)
            }

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

    /**
     * Adopts [session] and starts a new signature chain. [onSessionReset] runs on the message chain,
     * after the messages queued earlier.
     */
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
     * name.
     *
     * Without signature entries, every signable argument is unpacked without a signature, so a
     * player with a profile key, or any player while [SignedChatSettings.enforceSecureProfile] is
     * set, fails with [SignedMessageChain.DecodeException.MISSING_PROFILE_KEY]. Entries that do not
     * match the parsed arguments fail with
     * [SignedMessageChain.DecodeException.INVALID_COMMAND_SIGNATURE]; an unknown argument name also
     * breaks the signature chain.
     *
     * @throws SignedMessageChain.DecodeException when the command must not run
     */
    private fun getSignedArguments(
        packet: ClientSignedCommandChatPacket,
        lastSeenMessages: LastSeenMessages
    ): Map<String, PlayerChatMessage> {
        val entries = packet.signatures().entries()
        val values = signableCommandArguments(player, packet.message())
        val signedArguments = Object2ObjectOpenHashMap<String, PlayerChatMessage>(values.size)

        if (entries.isEmpty()) {
            values.forEach { (name, value) ->
                signedArguments[name] =
                    signedMessageDecoder.unpack(null, SignedMessageBody.unsigned(value))
            }

            return signedArguments
        }

        entries.forEach { entry ->
            val value = values[entry.name()]
            if (value == null) {
                signedMessageDecoder.setChainBroken()
                throw signedArgumentMismatch(packet, values)
            }

            val body = SignedMessageBody(
                content = value,
                timeStamp = Instant.ofEpochMilli(packet.timestamp()),
                salt = packet.salt(),
                lastSeen = lastSeenMessages
            )

            signedArguments[entry.name()] = signedMessageDecoder.unpack(entry.signature(), body)
        }

        if (values.keys.any { it !in signedArguments }) {
            throw signedArgumentMismatch(packet, values)
        }

        return signedArguments
    }

    private fun signedArgumentMismatch(
        packet: ClientSignedCommandChatPacket,
        expected: Map<String, String>
    ): SignedMessageChain.DecodeException {
        LOGGER.warn(
            "Signed command mismatch for {} ('{}'): got [{}] from the client, but expected [{}]",
            player.username,
            packet.message(),
            packet.signatures().entries().joinToString { it.name() },
            expected.keys.joinToString()
        )

        return SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.INVALID_COMMAND_SIGNATURE)
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

    /**
     * Whether the player's chat visibility setting allows player chat messages, which only `FULL`
     * does.
     */
    fun acceptsChatMessages(): Boolean = player.settings.chatMessageType() == ChatMessageType.FULL

    /**
     * Sends a signed chat message and records its signature for later last-seen references.
     *
     * Does nothing when the player does not accept chat messages. Kicks the player when more than
     * [MAX_PENDING_CHATS] messages are awaiting acknowledgement. Thread-safe.
     */
    fun sendPlayerChatMessage(message: PlayerChatMessage, chatType: BoundChatType) {
        if (!acceptsChatMessages()) return

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

    /**
     * Sends [content] as a chat message without sender. Does nothing when the player does not
     * accept chat messages.
     */
    fun sendDisguisedChatMessage(content: Component, chatType: BoundChatType) {
        if (!acceptsChatMessages()) return

        player.sendPacket(
            DisguisedChatPacket(content, chatType.id, chatType.name, chatType.targetName)
        )
    }

    /**
     * Kicks the player on the next tick, so the call is safe from any thread.
     */
    private fun disconnect(reason: Component) {
        player.scheduleNextTick { player.kick(reason) }
    }
}
