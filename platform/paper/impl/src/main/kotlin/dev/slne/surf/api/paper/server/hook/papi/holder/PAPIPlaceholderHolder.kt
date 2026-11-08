package dev.slne.surf.api.paper.server.hook.papi.holder

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion

interface PAPIPlaceholderHolder {
    val expansion: PapiExpansion

    fun registerHolder()
    fun unregisterHolder()
}