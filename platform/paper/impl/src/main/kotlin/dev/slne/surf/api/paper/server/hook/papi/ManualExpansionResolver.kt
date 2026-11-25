package dev.slne.surf.api.paper.server.hook.papi

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import org.bukkit.OfflinePlayer

/**
 * Resolves the requests of a [PapiExpansion] built by hand; requests without a player resolve to
 * `null`.
 */
internal class ManualExpansionResolver(
    private val expansion: PapiExpansion,
) : PlaceholderResolver {

    override fun resolve(player: OfflinePlayer?, params: String): String? {
        if (player == null) return null
        if (params.isEmpty()) return expansion.parseWithNoParams(player)

        val segments = params.split('_')
        val placeholder = expansion.placeholders[segments.first()] ?: return null

        return placeholder.parse(player, segments.drop(1))
    }

    companion object {

        /**
         * Describes [expansion] for the hook, with [expansion] as its owner.
         *
         * @param expansion the expansion to describe
         * @return the definition
         */
        fun define(expansion: PapiExpansion): ExpansionDefinition {
            return ExpansionDefinition(
                identifier = expansion.identifier,
                author = expansion.author,
                version = expansion.version,
                resolver = ManualExpansionResolver(expansion),
                owner = expansion,
            )
        }
    }
}
