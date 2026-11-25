package dev.slne.surf.api.paper.hook.papi

import org.bukkit.OfflinePlayer

/**
 * Resolves the requests of a [PapiExpansion] that no [Placeholder] method handles.
 *
 * Implemented by the class annotated with [PapiExpansion]; the classes in
 * [PapiExpansion.placeholders] are not consulted.
 */
interface PapiExpansionFallback {

    /**
     * Resolves a request without a placeholder name or with an unknown one.
     *
     * Called on the thread that resolves the placeholder, which may be any thread.
     *
     * @param player the requesting player, or `null` for a request without one
     * @param params everything after `<identifier>_`, empty for `%<identifier>_%`
     * @return the replacement, or `null` to leave the placeholder unresolved
     */
    fun onRequest(player: OfflinePlayer?, params: String): String?
}
