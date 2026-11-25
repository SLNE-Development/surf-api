package dev.slne.surf.api.paper.server.hook.papi

import org.bukkit.OfflinePlayer

/**
 * An expansion as [SurfPaperPAPIHookImpl] registers it, independent of how it was declared.
 *
 * @property identifier the expansion's identifier, unique across the hook
 * @property author the author reported to PlaceholderAPI
 * @property version the version reported to PlaceholderAPI
 * @property resolver resolves every request of the expansion
 * @property owner the object that declared the expansion; only the same owner unregisters it
 */
internal class ExpansionDefinition(
    val identifier: String,
    val author: String,
    val version: String,
    val resolver: PlaceholderResolver,
    val owner: Any,
)

/**
 * Resolves the requests of one expansion.
 *
 * Called on the thread that resolves the placeholder, which may be any thread.
 */
internal fun interface PlaceholderResolver {

    /**
     * Resolves a request.
     *
     * @param player the requesting player, or `null` for a request without one
     * @param params everything after `<identifier>_`, empty for `%<identifier>_%`
     * @return the replacement, or `null` to leave the placeholder unresolved
     */
    fun resolve(player: OfflinePlayer?, params: String): String?
}
