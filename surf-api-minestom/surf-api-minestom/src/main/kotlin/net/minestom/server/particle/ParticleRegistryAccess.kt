package net.minestom.server.particle

import net.minestom.server.registry.Registry

internal object ParticleRegistryAccess {
    @JvmStatic
    fun registry(): Registry<Particle> = ParticleImpl.REGISTRY
}