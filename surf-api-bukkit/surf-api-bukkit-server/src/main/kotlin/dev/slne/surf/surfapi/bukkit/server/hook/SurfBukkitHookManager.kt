package dev.slne.surf.surfapi.bukkit.server.hook

import dev.slne.surf.surfapi.bukkit.api.hook.papi.SurfBukkitPAPIHook
import dev.slne.surf.surfapi.bukkit.server.hook.papi.SurfBukkitPAPIHookImpl

object SurfBukkitHookManager {
    fun onEnable() {
        (SurfBukkitPAPIHook.INSTANCE as SurfBukkitPAPIHookImpl).onEnable()
    }
}