package dev.slne.surf.api.paper.server.hook.papi.holder

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PAPIPlaceholderHolderImpl(override val expansion: PapiExpansion) : PlaceholderExpansion(),
    PAPIPlaceholderHolder {
    override fun getIdentifier() = expansion.identifier
    override fun getAuthor() = expansion.author
    override fun getVersion() = expansion.version
    override fun persist() = true

    override fun onRequest(player: OfflinePlayer, params: String): String? {
        if (params.isEmpty()) {
            return expansion.parseWithNoParams(player)
        }

        val separator = params.indexOf('_')
        val placeholder = if (separator < 0) params else params.substring(0, separator)
        val papiPlaceholder = expansion.placeholders[placeholder] ?: return null
        val arguments = if (separator < 0) {
            emptyList()
        } else {
            params.substring(separator + 1).split('_')
        }

        return papiPlaceholder.parse(player, arguments)
    }

    override fun registerHolder() {
        register()
    }

    override fun unregisterHolder() {
        unregister()
    }
}
