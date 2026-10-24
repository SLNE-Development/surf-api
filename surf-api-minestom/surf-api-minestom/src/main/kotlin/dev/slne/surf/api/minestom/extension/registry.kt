package dev.slne.surf.api.minestom.extension

import net.kyori.adventure.key.Key
import net.minestom.server.particle.Particle
import net.minestom.server.particle.ParticleRegistryAccess
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey

/**
 * Returns the value registered under [key].
 *
 * @throws NoSuchElementException when nothing is registered under [key]
 */
fun <T> Registry<T>.getOrThrow(key: Key): T {
    return this.get(key) ?: notFound(key)
}

/**
 * Returns the value registered under the numeric [id].
 *
 * @throws NoSuchElementException when nothing is registered under [id]
 */
fun <T> Registry<T>.getOrThrow(id: Int): T {
    return this.get(id) ?: notFound(id)
}

/**
 * Returns the value registered under [key].
 *
 * @throws NoSuchElementException when nothing is registered under [key]
 */
fun <T> Registry<T>.getOrThrow(key: RegistryKey<T>): T {
    return this.get(key) ?: notFound(key.key())
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Registry<*>.notFound(key: Any): Nothing {
    throw NoSuchElementException("No value for '$key' in $this")
}

/**
 * The static registry backing [Particle]. Minestom does not expose it.
 */
val particleRegistry: Registry<Particle>
    get() = ParticleRegistryAccess.registry()
