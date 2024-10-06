package dev.slne.surf.surfapi.bukkit.server.hook.papi.holder

import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion
import me.clip.placeholderapi.expansion.PlaceholderExpansion
import org.bukkit.OfflinePlayer

class PAPIPlaceholderHolderImpl(override val expansion: PapiExpansion): PlaceholderExpansion(), PAPIPlaceholderHolder {
    override fun getIdentifier() = expansion.identifier
    override fun getAuthor() = expansion.author
    override fun getVersion() = expansion.version
    override fun persist() = true

    override fun onRequest(player: OfflinePlayer, params: String): String? =
        expansion.placeholders[params]?.parse(player, params.split("_"))

    override fun registerHolder() {
        register()
    }

    override fun unregisterHolder() {
        unregister()
    }
}