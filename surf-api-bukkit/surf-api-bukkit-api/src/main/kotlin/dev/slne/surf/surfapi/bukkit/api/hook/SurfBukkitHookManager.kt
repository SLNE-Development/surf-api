package dev.slne.surf.surfapi.bukkit.api.hook

import dev.slne.surf.surfapi.bukkit.api.SurfBukkitApi
import dev.slne.surf.surfapi.bukkit.api.hook.papi.SurfBukkitPAPIHook
import org.jetbrains.annotations.ApiStatus.NonExtendable

@NonExtendable
interface SurfBukkitHookManager {
    companion object {
        @JvmStatic
        fun get(): SurfBukkitHookManager = SurfBukkitApi.get().hookManager
    }

    val papiHook: SurfBukkitPAPIHook
}
