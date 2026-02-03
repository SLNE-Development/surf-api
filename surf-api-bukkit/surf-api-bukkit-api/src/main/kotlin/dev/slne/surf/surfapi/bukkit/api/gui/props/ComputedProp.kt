package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Computed prop - accepts a callback that computes the value.
 * The compute function is suspend to allow async operations.
 */
open class ComputedProp<T>(
    override val name: String,
    private val compute: suspend () -> T
) : Prop<T> {
    override suspend fun get(): T = compute()
}
