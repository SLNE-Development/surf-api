package dev.slne.surf.api.minestom.server.chat.impl

import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import java.text.MessageFormat
import java.util.Locale


internal object ChatTranslations {

    private val STORE_KEY = Key.key("surf", "chat")

    @Volatile
    private var registered = false

    fun register() {
        if (registered) return
        registered = true

        val store = TranslationStore.messageFormat(STORE_KEY)
        store.defaultLocale(Locale.US)

        store.registerAll(
            Locale.US,
            mapOf(
                "chat.type.text" to MessageFormat("<{0}> {1}"),
                "chat.type.announcement" to MessageFormat("[{0}] {1}"),
                "chat.type.emote" to MessageFormat("* {0} {1}"),
                "chat.type.team.text" to MessageFormat("{0} <{1}> {2}"),
                "chat.type.team.sent" to MessageFormat("-> {0} <{1}> {2}"),
                "commands.message.display.incoming" to MessageFormat("{0} whispers to you: {1}"),
                "commands.message.display.outgoing" to MessageFormat("You whisper to {0}: {1}"),
                "%s" to MessageFormat("{0}"),
            )
        )

        GlobalTranslator.translator().addSource(store)
    }
}
