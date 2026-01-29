package dev.slne.surf.surfapi.bukkit.server.component.papi

import com.google.auto.service.AutoService
import dev.slne.surf.surfapi.bukkit.api.component.papi.SurfBukkitPAPIComponent
import dev.slne.surf.surfapi.bukkit.api.component.papi.expansion.PapiExpansion
import dev.slne.surf.surfapi.bukkit.server.component.papi.holder.PAPIPlaceholderHolder
import dev.slne.surf.surfapi.bukkit.server.component.papi.holder.PAPIPlaceholderHolderImpl
import dev.slne.surf.surfapi.bukkit.server.component.papi.holder.PAPIPlaceholderHolderNOOP
import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf

@AutoService(SurfBukkitPAPIComponent::class)
class SurfBukkitPAPIComponentImpl : SurfBukkitPAPIComponent {
    private val expansionHolders = mutableObject2ObjectMapOf<String, PAPIPlaceholderHolder>()
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
        runCatching { Class.forName("me.clip.placeholderapi.PlaceholderAPI") }.isSuccess
    }
}
