package dev.slne.surf.api.minestom.server.impl.command

import net.kyori.adventure.key.Key
import net.kyori.adventure.translation.GlobalTranslator
import net.kyori.adventure.translation.TranslationStore
import java.text.MessageFormat
import java.util.*

/**
 * Server-side fallbacks for the vanilla command translation keys.
 */
object CommandAPITranslations {
    private val STORE_KEY = Key.key("surf", "commandapi")

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
                "command.unknown.command" to
                        MessageFormat("Unknown or incomplete command. See below for error"),
                "command.unknown.argument" to MessageFormat("Incorrect argument for command"),
                "command.context.here" to MessageFormat("<--[HERE]"),
            ),
        )

        GlobalTranslator.translator().addSource(store)
    }
}

