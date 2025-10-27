package dev.slne.surf.surfapi.bukkit.server.nms

import dev.slne.surf.surfapi.bukkit.server.reflection.Reflection
import net.minecraft.core.MappedRegistry
import net.minecraft.core.Registry
import net.minecraft.resources.ResourceKey

object RegistryLock {

    fun <T> getMappedRegistry(registry: ResourceKey<Registry<T>>): MappedRegistry<T> =
        dedicatedServer.registryAccess()
            .lookupOrThrow(registry)
                as? MappedRegistry ?: error("Registry $registry is not a MappedRegistry")


    inline fun <T, R> unlockRegistry(
        registry: ResourceKey<Registry<T>>,
        withUnlockedRegistry: (MappedRegistry<T>) -> R
    ): R {
        val registry = getMappedRegistry(registry)

        try {
            Reflection.MAPPED_REGISTRY_PROXY.setFrozen(registry, false)
            return withUnlockedRegistry(registry)
        } finally {
            registry.freeze()
        }
    }
}