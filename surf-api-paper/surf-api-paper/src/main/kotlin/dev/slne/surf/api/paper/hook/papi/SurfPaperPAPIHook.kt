package dev.slne.surf.api.paper.hook.papi

import dev.slne.surf.api.core.util.requiredService
import dev.slne.surf.api.paper.hook.papi.expansion.PapiExpansion

interface SurfPaperPAPIHook {
    fun register(expansion: PapiExpansion)
    fun unregister(expansion: PapiExpansion)

    companion object : SurfPaperPAPIHook by papiHook {
        val INSTANCE get() = papiHook
    }
}

private val papiHook = requiredService<SurfPaperPAPIHook>()