package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.extension.ChatTypeRegistry
import net.kyori.adventure.text.Component
import net.minestom.server.message.ChatType
import net.minestom.server.message.ChatTypeDecoration
import net.minestom.server.registry.RegistryKey
import net.kyori.adventure.chat.ChatType as AdventureChatType

/**
 * A chat type together with the sender name and optional target name it is rendered with.
 */
internal data class BoundChatType(
    val chatType: RegistryKey<ChatType>,
    val name: Component,
    val targetName: Component? = null
) {
    /**
     * The chat type ID of the chat packets: the registry ID plus one.
     *
     * @throws IllegalStateException If the chat type is not registered.
     */
    val id: Int
        get() {
            val registryId = ChatTypeRegistry.getId(chatType)
            check(registryId != -1) { "Chat type $chatType is not registered" }

            return registryId + 1
        }

    fun withTargetName(targetName: Component) = copy(targetName = targetName)

    fun adventure(): AdventureChatType.Bound =
        AdventureChatType.chatType(chatType.key()).bind(name, targetName)

    /**
     * Applies the decoration of the chat type to [content], for audiences that do not receive chat
     * packets. Returns [content] unchanged when the chat type has no registered decoration.
     */
    fun decorate(content: Component): Component {
        val decoration = ChatTypeRegistry.get(chatType)?.chat() ?: return content

        val arguments = decoration.parameters().map { parameter ->
            when (parameter) {
                ChatTypeDecoration.Parameter.SENDER -> name
                ChatTypeDecoration.Parameter.TARGET -> targetName ?: Component.empty()
                ChatTypeDecoration.Parameter.CONTENT -> content
            }
        }

        return Component.translatable(decoration.translationKey(), arguments)
            .style(decoration.style())
    }
}
