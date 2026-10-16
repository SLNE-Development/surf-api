package dev.slne.surf.api.minestom.server.impl.performance

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Section
import net.minestom.server.instance.light.Light
import net.minestom.server.instance.light.LightCompute
import net.minestom.server.instance.palette.Palette

/**
 * Sky light selection used by [dev.slne.surf.api.minestom.server.mixin.LightingChunkSkylightMixin].
 */
object SkylightSuppression {

    /**
     * The sky light of [section], or [DisabledSkyLight] when the dimension has no sky light.
     */
    @JvmStatic
    fun sectionSkyLight(hasSkylight: Boolean, section: Section): Light {
        return if (hasSkylight) section.skyLight() else DisabledSkyLight
    }

    /**
     * Sky light that never relights or sends and reports unset content, so no sky light mask bit is set.
     */
    @Suppress("UnstableApiUsage")
    object DisabledSkyLight : Light {

        override fun requiresUpdate() = false
        override fun requiresSend() = false
        override fun array(): ByteArray = LightCompute.UNSET_CONTENT
        override fun getLevel(x: Int, y: Int, z: Int) = 0

        override fun flip() = Unit
        override fun invalidate() = Unit
        override fun set(copyArray: ByteArray) = Unit

        override fun calculateInternal(
            blockPalette: Palette,
            chunkX: Int,
            chunkY: Int,
            chunkZ: Int,
            heightmap: IntArray,
            maxY: Int,
            lightLookup: Light.LightLookup,
        ): Set<Point> = emptySet()

        override fun calculateExternal(
            blockPalette: Palette,
            neighbors: Array<Point>,
            lightLookup: Light.LightLookup,
            paletteLookup: Light.PaletteLookup,
        ): Set<Point> = emptySet()
    }
}
