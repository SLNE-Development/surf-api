package dev.slne.surf.surfapi.bukkit.api.component.papi

import dev.slne.surf.surfapi.bukkit.api.component.papi.expansion.PapiExpansion
import dev.slne.surf.surfapi.core.api.util.requiredService

interface SurfBukkitPAPIComponent {
    fun register(expansion: PapiExpansion)
    fun unregister(expansion: PapiExpansion)

    companion object {
        @JvmStatic
        val instance = requiredService<SurfBukkitPAPIComponent>()
    }
}

val papiComponent get() = SurfBukkitPAPIComponent.instance
