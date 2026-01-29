package dev.slne.surf.surfapi.bukkit.server.component

import dev.slne.surf.surfapi.bukkit.api.component.papi.papiComponent
import dev.slne.surf.surfapi.bukkit.server.component.papi.SurfBukkitPAPIComponentImpl

object SurfBukkitComponentManager {
    fun onEnable() {
        (papiComponent as SurfBukkitPAPIComponentImpl).onEnable()
    }
}
