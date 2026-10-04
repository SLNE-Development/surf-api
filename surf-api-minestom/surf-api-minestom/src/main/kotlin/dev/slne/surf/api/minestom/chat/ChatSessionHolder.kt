package dev.slne.surf.api.minestom.chat

import dev.slne.surf.api.shared.api.util.InternalSurfApi
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.Player
import java.util.*

/**
 * The secure chat state of one player while the server runs signed chat.
 *
 * ```
 * val holder = ChatSessionHolder.fromPlayer(player) ?: return
 * holder.sendSignedMessage(event.signedMessage, player.name, rendered)
 * ```
 *
 * The extensions on [Player], such as [Player.sendSignedMessage], look the holder up themselves
 * and fall back to plain system messages when the server does not run signed chat.
 */
interface ChatSessionHolder {

    val player: Player

    /**
     * The chat session this player currently signs their messages under, or `null` while they
     * have none.
     */
    val chatSession: ChatSession?

    /**
     * Sends [message] to this player with its signature intact, attributed to [boundName].
     *
     * [unsignedContent] replaces what is displayed without touching what was signed. Passing
     * `null` shows the signed content as it was sent.
     *
     * A message that carries no signature of this server's own is sent as plain system text.
     */
    fun sendSignedMessage(
        message: SignedMessage,
        boundName: Component,
        unsignedContent: Component? = null,
    )

    /**
     * Captures [message], which this player sent, so that it can be shown to a player on another
     * server.
     *
     * [unsignedContent] replaces what is displayed there without touching what was signed.
     *
     * Returns `null` for a message this server did not receive from a client, which therefore
     * carries nothing another server could hand to a client.
     */
    fun captureSignedMessage(
        message: SignedMessage,
        unsignedContent: Component? = null,
    ): RemoteSignedMessage?

    /**
     * Shows [message] to this player as coming from [sender], a player on another server.
     *
     * [sender] is announced to this player's client for as long as it takes to deliver the
     * message, so that the signature can be verified without the sender ever being on this
     * server.
     *
     * @see captureSignedMessage
     */
    fun sendRemoteSignedMessage(
        sender: RemoteChatSender,
        message: RemoteSignedMessage,
        boundName: Component,
    )

    /** Deletes the message with [signature] from this player's chat. */
    fun deleteMessage(signature: SignedMessage.Signature)

    /**
     * Looks the holders up; installed by the server when it runs signed chat.
     */
    @InternalSurfApi
    interface Provider {
        fun fromPlayer(player: Player): ChatSessionHolder?
        fun fromUuid(uuid: UUID): ChatSessionHolder?

        /** Deletes a message from [player]'s chat, which works with or without signed chat. */
        fun deleteMessage(player: Player, signature: SignedMessage.Signature)
    }

    companion object {
        @Volatile
        private var provider: Provider? = null

        /**
         * The secure chat state of [player], or `null` when the server does not run signed chat
         * or the player is no longer online.
         */
        fun fromPlayer(player: Player): ChatSessionHolder? = provider?.fromPlayer(player)

        /**
         * The secure chat state of the online player [uuid], or `null` when the server does not
         * run signed chat or no such player is online.
         */
        fun fromUuid(uuid: UUID): ChatSessionHolder? = provider?.fromUuid(uuid)

        @InternalSurfApi
        fun installProvider(provider: Provider?) {
            this.provider = provider
        }

        internal fun deleteMessage(player: Player, signature: SignedMessage.Signature): Boolean {
            val provider = provider ?: return false
            provider.deleteMessage(player, signature)
            return true
        }
    }
}

/**
 * The secure chat state of this player.
 *
 * @see ChatSessionHolder.fromPlayer
 */
val Player.chatSessionHolder: ChatSessionHolder?
    get() = ChatSessionHolder.fromPlayer(this)

/**
 * The chat session this player currently signs their messages under, or `null` while they have
 * none or the server does not run signed chat.
 */
val Player.signedChatSession: ChatSession?
    get() = chatSessionHolder?.chatSession

/**
 * Sends [message] to this player with its signature intact, attributed to [boundName].
 *
 * Without signed chat, or for a message this server did not receive from a client, the message
 * is sent as plain system text: [unsignedContent], or else the text that was signed.
 *
 * @see ChatSessionHolder.sendSignedMessage
 */
fun Player.sendSignedMessage(
    message: SignedMessage,
    boundName: Component,
    unsignedContent: Component? = null,
) {
    val holder = chatSessionHolder
    if (holder == null) {
        sendMessage(unsignedContent ?: Component.text(message.message()))
        return
    }

    holder.sendSignedMessage(message, boundName, unsignedContent)
}

/**
 * Captures [message], which this player sent, so that it can be shown on another server.
 *
 * @return `null` when the server does not run signed chat or the message did not come from a
 * client
 * @see ChatSessionHolder.captureSignedMessage
 */
fun Player.captureSignedMessage(
    message: SignedMessage,
    unsignedContent: Component? = null,
): RemoteSignedMessage? = chatSessionHolder?.captureSignedMessage(message, unsignedContent)

/**
 * Shows [message] to this player as coming from [sender], a player on another server.
 *
 * Without signed chat, the message is sent as plain system text.
 *
 * @see ChatSessionHolder.sendRemoteSignedMessage
 */
fun Player.sendRemoteSignedMessage(
    sender: RemoteChatSender,
    message: RemoteSignedMessage,
    boundName: Component,
) {
    val holder = chatSessionHolder
    if (holder == null) {
        sendMessage(message.unsignedContent ?: Component.text(message.content))
        return
    }

    holder.sendRemoteSignedMessage(sender, message, boundName)
}

/**
 * Deletes the message with [signature] from this player's chat. Does nothing when the server
 * does not run signed chat, since then no message carries a signature.
 */
fun Player.deleteSignedMessage(signature: SignedMessage.Signature) {
    ChatSessionHolder.deleteMessage(this, signature)
}
