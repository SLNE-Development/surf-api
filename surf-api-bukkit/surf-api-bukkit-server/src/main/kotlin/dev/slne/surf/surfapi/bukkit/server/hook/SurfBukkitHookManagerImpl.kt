package dev.slne.surf.surfapi.bukkit.server.hook

import dev.slne.surf.surfapi.bukkit.api.hook.SurfBukkitHookManager
import dev.slne.surf.surfapi.bukkit.server.hook.papi.SurfBukkitPAPIHookImpl

class SurfBukkitHookManagerImpl : SurfBukkitHookManager {

    override val papiHook: SurfBukkitPAPIHookImpl = SurfBukkitPAPIHookImpl()

    fun onEnable() {
        papiHook.onEnable()
    }
}