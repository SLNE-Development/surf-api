package dev.slne.surf.api.minestom.chat

import net.minestom.server.crypto.ChatSession
import java.util.UUID

/**
 * The player a [RemoteSignedMessage] came from, as far as the receiving server knows them.
 *
 * [session] is the chat session the message was signed under. It is `null` for a sender that had
 * none, in which case the message is shown as unsigned.
 */
data class RemoteChatSender(
    val uuid: UUID,
    val username: String,
    val session: ChatSession?,
)
