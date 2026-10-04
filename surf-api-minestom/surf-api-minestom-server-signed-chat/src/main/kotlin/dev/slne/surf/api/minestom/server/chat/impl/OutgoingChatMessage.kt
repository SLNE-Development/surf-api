package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.server.chat.impl.signature.PlayerChatMessage
import net.kyori.adventure.text.Component


internal sealed interface OutgoingChatMessage {

    val content: Component

    fun sendToPlayer(
        handler: PlayerChatHandler,
        filtered: Boolean,
        chatType: BoundChatType,
        unsigned: Component? = null
    )


    data class Disguised(override val content: Component) : OutgoingChatMessage {
        override fun sendToPlayer(
            handler: PlayerChatHandler,
            filtered: Boolean,
            chatType: BoundChatType,
            unsigned: Component?
        ) {
            handler.sendDisguisedChatMessage(unsigned ?: content, chatType)
        }
    }


    data class Signed(val message: PlayerChatMessage) : OutgoingChatMessage {
        override val content: Component get() = message.decoratedContent()

        override fun sendToPlayer(
            handler: PlayerChatHandler,
            filtered: Boolean,
            chatType: BoundChatType,
            unsigned: Component?
        ) {
            var filteredMessage = message.filter(filtered)
            if (unsigned != null) {
                filteredMessage = filteredMessage.withUnsignedContent(unsigned)
            }

            if (!filteredMessage.isFullyFiltered()) {
                handler.sendPlayerChatMessage(filteredMessage, chatType)
            }
        }
    }

    companion object {
        fun create(message: PlayerChatMessage): OutgoingChatMessage =
            if (message.isSystem()) Disguised(message.decoratedContent()) else Signed(message)
    }
}
