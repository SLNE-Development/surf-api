package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.instance.Section
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SkylightSuppressionTest {

    @Test
    fun `a dimension with skylight keeps the sky light of the section`() {
        val section = Section()

        assertSame(
            section.skyLight(),
            SkylightSuppression.sectionSkyLight(hasSkylight = true, section = section),
        )
    }

    @Test
    fun `a dimension without skylight gets a light that needs neither relight nor send`() {
        val section = Section()
        section.invalidate()

        assertTrue(section.skyLight().requiresUpdate())

        val light = SkylightSuppression.sectionSkyLight(hasSkylight = false, section = section)

        assertNotSame(section.skyLight(), light)
        assertFalse(light.requiresUpdate())
        assertFalse(light.requiresSend())
        assertEquals(0, light.array().size, "an empty array keeps the section out of the sky mask")
    }

    @Test
    fun `the stand-in leaves the state of the section alone`() {
        val section = Section()
        section.invalidate()

        SkylightSuppression.sectionSkyLight(hasSkylight = false, section = section)

        // requiresSend() consumes a flag, so the stand-in must not have asked the real sky light
        assertTrue(section.skyLight().requiresSend())
        assertTrue(section.skyLight().requiresUpdate())
        assertTrue(section.blockLight().requiresUpdate())
    }
}
