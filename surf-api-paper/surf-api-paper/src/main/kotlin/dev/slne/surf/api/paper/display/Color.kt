package dev.slne.surf.api.paper.display

/**
 * Packs RGBA components into a single ARGB int.
 */
fun argb(r: Int, g: Int, b: Int, a: Int = 255): Int =
    (a shl 24) or (r shl 16) or (g shl 8) or b

/**
 * Converts an RGB int (0xRRGGBB) to a fully opaque ARGB int.
 */
fun argb(rgb: Int): Int = (0xFF shl 24) or (rgb and 0xFFFFFF)

/**
 * Convenience alias for [argb] from an RGB hex value.
 */
fun color(rgb: Int): Int = argb(rgb)

/**
 * Convenience alias for [argb] from individual RGB components.
 */
fun color(r: Int, g: Int, b: Int): Int = argb(r, g, b)

/**
 * Convenience alias for [argb] from individual RGBA components.
 */
fun color(r: Int, g: Int, b: Int, a: Int): Int = argb(r, g, b, a)
