package dev.slne.surf.surfapi.bukkit.server.component.papi.holder

import dev.slne.surf.surfapi.bukkit.api.component.papi.expansion.PapiExpansion

interface PAPIPlaceholderHolder {
    val expansion: PapiExpansion

    fun registerHolder()
    fun unregisterHolder()
}
