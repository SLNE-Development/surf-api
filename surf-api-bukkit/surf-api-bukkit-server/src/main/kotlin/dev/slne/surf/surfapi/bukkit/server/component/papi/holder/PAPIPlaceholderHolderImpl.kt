package dev.slne.surf.surfapi.bukkit.server.component.papi.holder

import dev.slne.surf.surfapi.bukkit.api.component.papi.expansion.PapiExpansion
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PAPIPlaceholderHolderImpl(override val expansion: PapiExpansion): PlaceholderExpansion(), PAPIPlaceholderHolder {
    override fun getIdentifier() = expansion.identifier
    override fun getAuthor() = expansion.author
    override fun getVersion() = expansion.version
    override fun persist() = true

    override fun onRequest(player: OfflinePlayer, params: String): String? {
        val parsedParams = params.split("_")

        if (parsedParams.isEmpty()) {
            return expansion.parseWithNoParams(player)
        }

        val placeholder = parsedParams.first()
        val papiPlaceholder = expansion.placeholders[placeholder] ?: return null

        return papiPlaceholder.parse(player, parsedParams.drop(1))
    }

    override fun registerHolder() {
        register()
    }

    override fun unregisterHolder() {
        unregister()
    }
}
