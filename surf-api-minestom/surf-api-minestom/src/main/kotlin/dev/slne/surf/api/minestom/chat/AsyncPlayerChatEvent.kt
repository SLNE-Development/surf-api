package dev.slne.surf.api.minestom.chat

import dev.slne.surf.api.minestom.event.SuspendingEventNode
import net.minestom.server.entity.Player
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.ApiStatus

/**
 * Fired when a [Player] sends a chat message.
 *
 * Listeners run off the tick thread on
 * a virtual thread, so they may suspend *and* they may block
 * without affecting the server's tick rate.
 *
 * Messages from one player are processed strictly in order, so a slow listener delays that player's
 * next message.
 *
 * ```
 * AsyncPlayerChatEvent.addListener { event ->
 *     val prefix = database.loadPrefix(event.player.uuid) // suspending or blocking, both fine
 *     event.renderer = ChatRenderer.viewerUnaware { _, name, message ->
 *         Component.text().append(prefix).append(name).append(Component.text(": ")).append(message).build()
 *     }
 * }
 * ```
 */
class AsyncPlayerChatEvent @ApiStatus.Internal constructor(

    val player: Player,

    /**
     * The [Audience]s that this chat message will be displayed to.
     *
     * Can be modified to add and remove viewers.
     */
    val viewers: MutableSet<Audience>,

    /**
     * The renderer used to turn [message] into the component every viewer sees.
     */
    var renderer: ChatRenderer,

    /**
     * The user-supplied message.
     */
    var message: Component,

    /**
     * The original and unmodified user-supplied message.
     *
     * The value will **not** reflect changes made through [message].
     */
    val originalMessage: Component,

    /**
     * The signed message backing this event.
     *
     * Changes made in this event will **not** update the signed message.
     */
    val signedMessage: SignedMessage,
) {

    var isCancelled = false

    companion object {
        @ApiStatus.Internal
        val node = SuspendingEventNode<AsyncPlayerChatEvent>("async-chat")

        typealias AsyncPlayerChatEventListener = suspend (AsyncPlayerChatEvent) -> Unit

        /**
         * Registers a chat listener. Lower [priority] runs first.
         *
         * @see SuspendingEventNode.addListener
         */
        fun addListener(
            priority: Int = 0,
            listener: AsyncPlayerChatEventListener
        ) = node.addListener(priority, listener)
    }
}
