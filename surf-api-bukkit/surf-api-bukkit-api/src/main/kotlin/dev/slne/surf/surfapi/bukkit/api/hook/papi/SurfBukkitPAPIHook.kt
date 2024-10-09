package dev.slne.surf.surfapi.bukkit.api.hook.papi

import dev.slne.surf.surfapi.bukkit.api.hook.SurfBukkitHookManager
import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion
import org.jetbrains.annotations.ApiStatus.NonExtendable

@NonExtendable
interface SurfBukkitPAPIHook {

    companion object {
        @JvmStatic
        fun get(): SurfBukkitPAPIHook = SurfBukkitHookManager.get().papiHook
    }

    fun register(expansion: PapiExpansion)
    fun unregister(expansion: PapiExpansion)
}