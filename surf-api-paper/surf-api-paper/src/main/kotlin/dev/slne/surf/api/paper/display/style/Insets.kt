package dev.slne.surf.api.paper.display.style

/**
 * Represents insets (padding/margin) for all four sides.
 */
data class Insets(
    val top: Int = 0,
    val right: Int = 0,
    val bottom: Int = 0,
    val left: Int = 0
) {
    val horizontal get() = left + right
    val vertical get() = top + bottom

    companion object {
        val ZERO = Insets()
        fun all(value: Int) = Insets(value, value, value, value)
        fun symmetric(vertical: Int, horizontal: Int) = Insets(vertical, horizontal, vertical, horizontal)
        fun horizontal(value: Int) = Insets(0, value, 0, value)
        fun vertical(value: Int) = Insets(value, 0, value, 0)
    }
}
