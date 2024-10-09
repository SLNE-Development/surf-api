package dev.slne.surf.surfapi.bukkit.server.hook.papi.holder

import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion

interface PAPIPlaceholderHolder {
    val expansion: PapiExpansion

    fun registerHolder()
    fun unregisterHolder()
}