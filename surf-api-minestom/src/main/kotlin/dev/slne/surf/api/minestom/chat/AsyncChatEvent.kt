package dev.slne.surf.api.minestom.chat

import dev.slne.surf.api.minestom.event.SuspendingEventNode
import dev.slne.surf.api.minestom.player.SurfMinestomPlayer
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import org.jetbrains.annotations.ApiStatus

/**
 * Fired when a [SurfMinestomPlayer] sends a chat message.
 *
 * Listeners run off the tick thread on
 * a virtual thread, so they may suspend *and* they may block
 * without affecting the server's tick rate.
 *
 * Messages from one player are processed strictly in order, so a slow listener delays that player's
 * next message.
 *
 * ```
 * AsyncChatEvent.addListener { event ->
 *     val prefix = database.loadPrefix(event.player.uuid) // suspending or blocking, both fine
 *     event.renderer = ChatRenderer.viewerUnaware { _, name, message ->
 *         Component.text().append(prefix).append(name).append(Component.text(": ")).append(message).build()
 *     }
 * }
 * ```
 */
class AsyncChatEvent @ApiStatus.Internal constructor(

    val player: SurfMinestomPlayer,

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
        val node = SuspendingEventNode<AsyncChatEvent>("async-chat")

        typealias AsyncChatEventListener = suspend (AsyncChatEvent) -> Unit

        /**
         * Registers a chat listener. Lower [priority] runs first.
         *
         * @see SuspendingEventNode.addListener
         */
        fun addListener(
            priority: Int = 0,
            listener: AsyncChatEventListener
        ) = node.addListener(priority, listener)
    }
}
