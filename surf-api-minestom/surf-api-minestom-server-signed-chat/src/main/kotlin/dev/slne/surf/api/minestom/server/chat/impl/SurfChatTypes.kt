package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.extension.ChatTypeRegistry
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.Style
import net.minestom.server.message.ChatType
import net.minestom.server.message.ChatTypeDecoration
import net.minestom.server.registry.RegistryKey


internal object SurfChatTypes {

    private val RAW_KEY = Key.key("surf", "raw")

    lateinit var raw: RegistryKey<ChatType>
        private set

    /**
     * Idempotent against the registry rather than a local flag, so a cached key from a previous
     * server instance cannot survive into a new one and resolve to id -1.
     */
    fun register() {
        ChatTypeRegistry.getKey(RAW_KEY)?.let {
            raw = it
            return
        }

        val decoration = ChatTypeDecoration(
            "%s",
            listOf(ChatTypeDecoration.Parameter.CONTENT),
            Style.empty()
        )

        raw = ChatTypeRegistry.register(
            RAW_KEY,
            ChatType.create(decoration, decoration)
        )
    }
}
