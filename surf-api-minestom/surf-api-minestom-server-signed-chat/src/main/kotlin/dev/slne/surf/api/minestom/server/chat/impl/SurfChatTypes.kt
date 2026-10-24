package dev.slne.surf.api.minestom.server.chat.impl

import dev.slne.surf.api.minestom.extension.ChatTypeRegistry
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.format.Style
import net.minestom.server.message.ChatType
import net.minestom.server.message.ChatTypeDecoration
import net.minestom.server.registry.RegistryKey


/**
 * The chat types the surf api adds to the registry.
 */
internal object SurfChatTypes {

    private val RAW_KEY = Key.key("surf", "raw")

    /**
     * The `surf:raw` chat type, which shows only the message content. Available after [register].
     */
    lateinit var raw: RegistryKey<ChatType>
        private set

    /**
     * Registers [raw] unless the registry already contains it.
     *
     * Checks the registry instead of a local flag so that a key cached from an earlier server
     * instance cannot outlive its registry and resolve to ID -1.
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
