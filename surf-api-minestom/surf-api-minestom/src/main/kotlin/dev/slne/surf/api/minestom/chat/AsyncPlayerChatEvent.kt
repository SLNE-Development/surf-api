package dev.slne.surf.api.minestom.chat

import dev.slne.surf.api.minestom.event.SuspendingEventNode
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.chat.SignedMessage
import net.kyori.adventure.text.Component
import net.minestom.server.entity.Player
import org.jetbrains.annotations.ApiStatus

/**
 * Fired when a [Player] sends a chat message.
 *
 * Listeners run off the tick thread and may suspend or block without affecting the tick rate.
 * Messages from one player are processed in order, so a slow listener delays that player's next
 * message.
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
     * The [Audience]s the message is sent to. Add or remove entries to change the recipients.
     */
    val viewers: MutableSet<Audience>,

    /**
     * Renders [message] into the component shown to each viewer.
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

    /**
     * Whether the message is dropped instead of being sent to [viewers].
     */
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
