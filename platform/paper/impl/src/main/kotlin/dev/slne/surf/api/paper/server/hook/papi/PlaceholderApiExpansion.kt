package dev.slne.surf.api.paper.server.hook.papi

import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

/**
 * The PlaceholderAPI expansion of one [ExpansionDefinition].
 */
internal class PlaceholderApiExpansion(
    private val definition: ExpansionDefinition,
) : PlaceholderExpansion(), ExpansionHolder {

    override fun getIdentifier(): String = definition.identifier

    override fun getAuthor(): String = definition.author

    override fun getVersion(): String = definition.version

    override fun persist(): Boolean = true

    override fun onRequest(player: OfflinePlayer?, params: String): String? {
        return definition.resolver.resolve(player, params)
    }

    override fun attach() {
        check(register()) { "PlaceholderAPI rejected the expansion '${definition.identifier}'" }
    }

    override fun detach() {
        unregister()
    }
}
