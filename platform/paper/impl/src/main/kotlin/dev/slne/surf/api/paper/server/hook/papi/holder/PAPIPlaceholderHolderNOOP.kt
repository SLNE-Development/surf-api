package dev.slne.surf.api.paper.server.hook.papi.holder

import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion

class PAPIPlaceholderHolderNOOP(override val expansion: PapiExpansion) : PAPIPlaceholderHolder {
    override fun registerHolder() {
        // NOOP
    }

    override fun unregisterHolder() {
        // NOOP
    }
}