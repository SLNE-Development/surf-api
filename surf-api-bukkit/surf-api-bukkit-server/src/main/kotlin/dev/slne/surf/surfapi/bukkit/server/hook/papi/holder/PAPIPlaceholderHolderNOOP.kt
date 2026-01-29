package dev.slne.surf.surfapi.bukkit.server.hook.papi.holder

import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion

class PAPIPlaceholderHolderNOOP(override val expansion: PapiExpansion) : PAPIPlaceholderHolder {
    override fun registerHolder() {
        // NOOP
    }

    override fun unregisterHolder() {
        // NOOP
    }
}