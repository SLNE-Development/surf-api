package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.extension.ChatTypeRegistry
import net.kyori.adventure.text.Component
import net.minestom.server.message.ChatType
import net.minestom.server.message.ChatTypeDecoration
import net.minestom.server.registry.RegistryKey
import net.kyori.adventure.chat.ChatType as AdventureChatType

internal data class BoundChatType(
    val chatType: RegistryKey<ChatType>,
    val name: Component,
    val targetName: Component? = null
) {
    /**
     * The value the chat packets carry for this chat type.
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
