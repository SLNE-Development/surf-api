package dev.slne.surf.surfapi.bukkit.server.hook

import dev.slne.surf.surfapi.bukkit.api.hook.papi.papiHook
import dev.slne.surf.surfapi.bukkit.server.hook.papi.SurfBukkitPAPIHookImpl

object SurfBukkitHookManager {
    fun onEnable() {
        (papiHook as SurfBukkitPAPIHookImpl).onEnable()
    }
}