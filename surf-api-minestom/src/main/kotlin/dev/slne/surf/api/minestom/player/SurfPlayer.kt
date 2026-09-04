package dev.slne.surf.api.minestom.player

import dev.slne.surf.api.minestom.chat.RemoteChatSender
import dev.slne.surf.api.minestom.chat.RemoteSignedMessage
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.crypto.ChatSession
import net.minestom.server.entity.Player
import net.minestom.server.network.player.GameProfile
import net.minestom.server.network.player.PlayerConnection
import org.jetbrains.annotations.ApiStatus

abstract class SurfPlayer @ApiStatus.Internal protected constructor(
    playerConnection: PlayerConnection,
    gameProfile: GameProfile,
) : Player(playerConnection, gameProfile) {

    abstract fun hasPermission(permission: String): Boolean

    /**
     * Sends [message] to this player with its signature intact, attributed to [boundName].
     *
     * [unsignedContent] replaces what is displayed without
     * touching what was signed. Passing `null`
     * shows the signed content as it was sent.
     *
     * A message that carries no signature of this server's own is sent as plain system text.
     */
    abstract fun sendSignedMessage(
        message: SignedMessage,
        boundName: Component,
        unsignedContent: Component?,
    )

    /**
     * Captures [message] so that it can be shown to a player on another server.
     *
     * [unsignedContent] replaces what is displayed there without touching what was signed.
     *
     * Returns `null` for a message this server did not receive from a client, which therefore
     * carries nothing another server could hand to a client.
     */
    abstract fun captureSignedMessage(
        message: SignedMessage,
        unsignedContent: Component?,
    ): RemoteSignedMessage?

    /**
     * The chat session this player currently signs their messages under, or `null` while they have
     * none.
     */
    abstract fun chatSession(): ChatSession?

    /**
     * Shows [message] to this player as coming from [sender], a player on another server.
     *
     * [sender] is announced to this player's client for as long as it takes to deliver the message,
     * so that the signature can be verified without the sender ever being on this server.
     *
     * @see captureSignedMessage
     */
    abstract fun sendRemoteSignedMessage(
        sender: RemoteChatSender,
        message: RemoteSignedMessage,
        boundName: Component,
    )

    fun displayName(): Component = (displayName ?: Component.text(username))
        .insertion(username)
        .hoverEvent(this)
}