package dev.slne.surf.api.minestom.extension

import dev.slne.surf.api.minestom.extension.getOrThrow
import dev.slne.surf.api.minestom.extension.particleRegistry
import net.kyori.adventure.key.Key
import net.minestom.server.particle.Particle
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class ParticleRegistryTest {

    @Test
    fun `the exposed registry is the one backing Particle`() {
        val flame = requireNotNull(Particle.fromKey("minecraft:flame"))

        assertEquals(Particle.values().size, particleRegistry.size())
        assertSame(flame, particleRegistry.getOrThrow(Key.key("flame")))
        assertSame(flame, particleRegistry.getOrThrow(flame.id()))
        assertSame(flame, particleRegistry.getOrThrow(flame.registryKey()))
    }

    @Test
    fun `an unknown key is reported as missing`() {
        assertThrows(NoSuchElementException::class.java) {
            particleRegistry.getOrThrow(Key.key("surf", "not_a_particle"))
        }
    }
}
