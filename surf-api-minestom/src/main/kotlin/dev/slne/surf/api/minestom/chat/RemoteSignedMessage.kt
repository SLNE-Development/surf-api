package dev.slne.surf.api.minestom.chat

import net.kyori.adventure.text.Component
import net.minestom.server.crypto.MessageSignature
import java.time.Instant
import java.util.UUID

/**
 * A signed chat message in a form that survives leaving this server.
 *
 * It carries everything a client needs to verify the signature, so a server that never saw the
 * sender can still show the message as signed. A message from a sender without a chat session has no
 * [signature], a nil [sessionId] and is shown as unsigned.
 *
 * @see dev.slne.surf.api.minestom.player.SurfPlayer.captureSignedMessage
 * @see dev.slne.surf.api.minestom.player.SurfPlayer.sendRemoteSignedMessage
 */
data class RemoteSignedMessage(
    val sender: UUID,
    val sessionId: UUID,

    /** The position of the message in the signature chain of its session. */
    val index: Int,

    val signature: MessageSignature?,

    /** The message as it was signed, which is the plain text the sender typed. */
    val content: String,
    val timestamp: Instant,
    val salt: Long,

    /** The signatures of the messages the sender had seen, which the signature also covers. */
    val lastSeen: List<MessageSignature>,

    /** What the receiver sees instead of [content]. */
    val unsignedContent: Component?,
)
