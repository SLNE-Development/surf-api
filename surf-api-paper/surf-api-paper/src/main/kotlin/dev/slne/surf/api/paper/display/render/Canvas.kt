package dev.slne.surf.api.paper.display.render

import org.bukkit.map.MapPalette
import java.awt.Color

/**
 * Low-level pixel buffer. Provides only primitive drawing operations.
 * Higher-level drawing (circles, lines, etc.) is handled by [dev.slne.surf.api.paper.display.shape.Shape] implementations.
 */
class Canvas(val width: Int, val height: Int) {
    val pixels = IntArray(width * height)

    // --- Clip Stack (for overflow clipping) ---
    private var clipX1 = 0
    private var clipY1 = 0
    private var clipX2 = width
    private var clipY2 = height
    private val clipStack = ArrayDeque<IntArray>()

    /**
     * Push a clip rectangle onto the stack. The effective clip is the intersection
     * of the current clip and the new rectangle. All drawing operations respect this.
     */
    fun pushClip(x: Int, y: Int, w: Int, h: Int) {
        clipStack.addLast(intArrayOf(clipX1, clipY1, clipX2, clipY2))
        clipX1 = maxOf(clipX1, x)
        clipY1 = maxOf(clipY1, y)
        clipX2 = minOf(clipX2, x + w)
        clipY2 = minOf(clipY2, y + h)
    }

    /** Pop the last clip rectangle, restoring the previous clip. */
    fun popClip() {
        val prev = clipStack.removeLastOrNull() ?: return
        clipX1 = prev[0]; clipY1 = prev[1]; clipX2 = prev[2]; clipY2 = prev[3]
    }

    /** Set a single pixel. Coordinates outside the clip are silently ignored. */
    fun setPixel(x: Int, y: Int, color: Int) {
        if (x in clipX1 until clipX2 && y in clipY1 until clipY2) {
            pixels[y * width + x] = color
        }
    }

    /** Set a single pixel, ignoring the clip stack (used for cursor overlay). */
    fun setPixelUnclipped(x: Int, y: Int, color: Int) {
        if (x in 0 until width && y in 0 until height) {
            pixels[y * width + x] = color
        }
    }

    /** Get a single pixel color. Returns 0 for out-of-bounds coordinates. */
    fun getPixel(x: Int, y: Int): Int {
        return if (x in 0 until width && y in 0 until height) pixels[y * width + x] else 0
    }

    /** Fill the entire canvas with a single color. */
    fun fill(color: Int) {
        pixels.fill(color)
    }

    /** Fill a rectangular area with a single color, respecting the clip. */
    fun fillRect(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val x1 = maxOf(clipX1, x)
        val y1 = maxOf(clipY1, y)
        val x2 = minOf(clipX2, x + w)
        val y2 = minOf(clipY2, y + h)
        for (py in y1 until y2) {
            val rowStart = py * width
            for (px in x1 until x2) {
                pixels[rowStart + px] = color
            }
        }
    }

    /** Fill a rectangular area with alpha blending, respecting the clip. */
    fun fillRectBlended(x: Int, y: Int, w: Int, h: Int, color: Int) {
        val srcA = (color ushr 24) and 0xFF
        if (srcA == 255) {
            fillRect(x, y, w, h, color)
            return
        }
        if (srcA == 0) return

        val x1 = maxOf(clipX1, x)
        val y1 = maxOf(clipY1, y)
        val x2 = minOf(clipX2, x + w)
        val y2 = minOf(clipY2, y + h)
        for (py in y1 until y2) {
            val rowStart = py * width
            for (px in x1 until x2) {
                pixels[rowStart + px] = alphaBlend(color, pixels[rowStart + px])
            }
        }
    }

    /** Draw a rectangular outline with a given thickness. */
    fun drawRect(x: Int, y: Int, w: Int, h: Int, color: Int, thickness: Int = 1) {
        fillRect(x, y, w, thickness, color)
        fillRect(x, y + h - thickness, w, thickness, color)
        fillRect(x, y, thickness, h, color)
        fillRect(x + w - thickness, y, thickness, h, color)
    }

    /** Copy another canvas onto this one at the given position (alpha-aware). */
    fun place(other: Canvas, destX: Int, destY: Int) {
        for (sy in 0 until other.height) {
            for (sx in 0 until other.width) {
                val pixel = other.pixels[sy * other.width + sx]
                if ((pixel ushr 24) > 0) {
                    setPixel(destX + sx, destY + sy, pixel)
                }
            }
        }
    }

    /**
     * Blend another canvas onto this one using alpha compositing (Source Over).
     * Used for rendering modal overlays with semi-transparent backgrounds.
     */
    fun blend(other: Canvas, destX: Int, destY: Int) {
        for (sy in 0 until other.height) {
            for (sx in 0 until other.width) {
                val srcColor = other.pixels[sy * other.width + sx]
                val srcA = (srcColor ushr 24) and 0xFF
                if (srcA == 0) continue

                val dx = destX + sx
                val dy = destY + sy
                if (dx !in 0 until width || dy !in 0 until height) continue

                if (srcA == 255) {
                    pixels[dy * width + dx] = srcColor
                } else {
                    pixels[dy * width + dx] = alphaBlend(srcColor, pixels[dy * width + dx])
                }
            }
        }
    }

    /**
     * Extracts a 128x128 tile of map color data from this canvas at the given pixel offset.
     * Used for converting to Minecraft map format.
     */
    @Suppress("DEPRECATION")
    fun toMapColors(offsetX: Int, offsetY: Int): ByteArray {
        val data = ByteArray(128 * 128)
        for (y in 0 until 128) {
            for (x in 0 until 128) {
                val px = offsetX + x
                val py = offsetY + y
                val argb = if (px in 0 until width && py in 0 until height) {
                    pixels[py * width + px]
                } else {
                    0
                }
                val alpha = (argb ushr 24) and 0xFF
                data[y * 128 + x] = if (alpha < 128) {
                    0
                } else {
                    MapPalette.matchColor(
                        Color((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
                    )
                }
            }
        }
        return data
    }

    companion object {
        /**
         * Alpha-blend source color over destination color (Source Over compositing).
         */
        fun alphaBlend(src: Int, dst: Int): Int {
            val srcA = (src ushr 24) and 0xFF
            if (srcA == 255) return src
            if (srcA == 0) return dst

            val srcR = (src shr 16) and 0xFF
            val srcG = (src shr 8) and 0xFF
            val srcB = src and 0xFF

            val dstA = (dst ushr 24) and 0xFF
            val dstR = (dst shr 16) and 0xFF
            val dstG = (dst shr 8) and 0xFF
            val dstB = dst and 0xFF

            val invSrcA = 255 - srcA
            val outA = srcA + (dstA * invSrcA) / 255
            if (outA == 0) return 0

            val outR = (srcR * srcA + dstR * dstA * invSrcA / 255) / outA
            val outG = (srcG * srcA + dstG * dstA * invSrcA / 255) / outA
            val outB = (srcB * srcA + dstB * dstA * invSrcA / 255) / outA

            return (outA shl 24) or (outR.coerceIn(0, 255) shl 16) or
                    (outG.coerceIn(0, 255) shl 8) or outB.coerceIn(0, 255)
        }
    }
}
