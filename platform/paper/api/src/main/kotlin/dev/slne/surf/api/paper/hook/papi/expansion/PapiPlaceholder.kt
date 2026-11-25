package dev.slne.surf.api.paper.hook.papi.expansion

import dev.slne.surf.api.paper.hook.papi.requirePlaceholderName
import org.bukkit.OfflinePlayer

/**
 * One placeholder of a [PapiExpansion] built by hand.
 *
 * @property placeholder the placeholder's name; it must not be empty or contain `%`, `{`, `}` or
 * `_`
 */
abstract class PapiPlaceholder(
    val placeholder: String,
) {
    init {
        requirePlaceholderName(placeholder, "Placeholder name")
    }

    /**
     * Resolves this placeholder for [player].
     *
     * Called on the thread that resolves the placeholder, which may be any thread.
     *
     * @param player the requesting player
     * @param args the request's segments after the placeholder name, split at `_`
     * @return the replacement, or `null` to leave the placeholder unresolved
     */
    abstract fun parse(player: OfflinePlayer, args: List<String>): String?
}
