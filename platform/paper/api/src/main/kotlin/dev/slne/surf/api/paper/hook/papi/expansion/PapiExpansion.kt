@file:Suppress("UnstableApiUsage")

package dev.slne.surf.api.paper.hook.papi.expansion

import dev.slne.surf.api.paper.hook.papi.requirePlaceholderName
import dev.slne.surf.api.paper.util.getCallingPlugin
import org.bukkit.OfflinePlayer

/**
 * A PlaceholderAPI expansion built by hand and registered through
 * [dev.slne.surf.api.paper.hook.papi.SurfPaperPAPIHook].
 *
 * A request `%<identifier>_<placeholder>_<arg>_<arg>%` calls the [PapiPlaceholder] named
 * `<placeholder>` with the remaining segments split at `_`; `%<identifier>_%` calls
 * [parseWithNoParams]. Requests without a player resolve to `null`. PlaceholderAPI never
 * resolves `%<identifier>%` without an underscore.
 *
 * A bean of a plugin's context declares its expansion with
 * [dev.slne.surf.api.paper.hook.papi.PapiExpansion] instead.
 *
 * @property identifier the expansion's identifier; it must not be empty or contain `%`, `{`, `}`
 * or `_`
 * @property placeholder the placeholders, keyed by [PapiPlaceholder.placeholder]
 * @property author the expansion's author; defaults to the authors of the constructing plugin
 * @property version the expansion's version; defaults to the version of the constructing plugin
 * @property name the expansion's display name
 */
open class PapiExpansion(
    val identifier: String,
    val placeholder: List<PapiPlaceholder>,
    val author: String = getCallingPlugin(2).pluginMeta.authors.joinToString(", "),
    val version: String = getCallingPlugin(2).pluginMeta.version,
    val name: String = identifier,
) {
    init {
        requirePlaceholderName(identifier, "Expansion identifier")
    }

    /**
     * The placeholders keyed by name.
     */
    val placeholders: Map<String, PapiPlaceholder> = placeholder.associateBy { it.placeholder }

    /**
     * Resolves `%<identifier>_%`, the request without parameters.
     *
     * Called on the thread that resolves the placeholder, which may be any thread.
     *
     * @param player the requesting player
     * @return the replacement, or `null` to leave the placeholder unresolved
     */
    open fun parseWithNoParams(player: OfflinePlayer): String? = null
}
