package dev.slne.surf.api.paper.server.hook

import dev.slne.surf.api.paper.hook.papi.SurfPaperPAPIHook
import dev.slne.surf.api.paper.server.hook.papi.SurfPaperPAPIHookImpl

object SurfBukkitHookManager {
    fun onEnable() {
        (SurfPaperPAPIHook.INSTANCE as SurfPaperPAPIHookImpl).onEnable()
    }
}