package dev.slne.surf.surfapi.bukkit.api.hook.papi

import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion
import dev.slne.surf.surfapi.core.api.util.requiredService

interface SurfBukkitPAPIHook {
    fun register(expansion: PapiExpansion)
    fun unregister(expansion: PapiExpansion)

    companion object {
        val instance = requiredService<SurfBukkitPAPIHook>()
    }
}

val papiHook get() = SurfBukkitPAPIHook.instance