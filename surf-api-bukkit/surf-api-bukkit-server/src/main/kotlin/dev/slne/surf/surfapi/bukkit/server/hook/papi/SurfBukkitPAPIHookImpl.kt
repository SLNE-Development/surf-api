package dev.slne.surf.surfapi.bukkit.server.hook.papi

import dev.slne.surf.surfapi.bukkit.api.hook.papi.SurfBukkitPAPIHook
import dev.slne.surf.surfapi.bukkit.api.hook.papi.expansion.PapiExpansion
import dev.slne.surf.surfapi.bukkit.server.hook.papi.holder.PAPIPlaceholderHolder
import dev.slne.surf.surfapi.bukkit.server.hook.papi.holder.PAPIPlaceholderHolderImpl
import dev.slne.surf.surfapi.bukkit.server.hook.papi.holder.PAPIPlaceholderHolderNOOP

class SurfBukkitPAPIHookImpl: SurfBukkitPAPIHook {
    private val expansionHolders = mutableMapOf<String, PAPIPlaceholderHolder>()
    private var loaded = false

    override fun register(expansion: PapiExpansion) {
        require(expansion.identifier !in expansionHolders) {
            "Expansion with identifier ${expansion.identifier} is already registered"
        }

        val holder = if (papiLoaded) {
            PAPIPlaceholderHolderImpl(expansion)
        } else {
            PAPIPlaceholderHolderNOOP(expansion)
        }

        expansionHolders[expansion.identifier] = holder

        if (loaded) {
            holder.registerHolder()
        }
    }

    override fun unregister(expansion: PapiExpansion) {
        val holder = expansionHolders.remove(expansion.identifier) ?: return
        if (loaded) {
            holder.unregisterHolder()
        }
    }

    fun onEnable() {
        loaded = true
        expansionHolders.values.forEach { it.registerHolder() }
    }

    private val papiLoaded by lazy {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI")
            true
        } catch (e: ClassNotFoundException) {
            false
        }
    }
}