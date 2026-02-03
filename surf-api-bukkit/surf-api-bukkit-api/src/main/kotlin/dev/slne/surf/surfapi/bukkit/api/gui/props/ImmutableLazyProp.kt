package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Immutable lazy prop - gets available when accessed, using a callback.
 * Cannot be modified after initialization.
 */
class ImmutableLazyProp<T>(
    override val name: String,
    private val initializer: () -> T
) : Prop<T> {
    private val value = lazy { initializer() }
    
    override suspend fun get(): T = value.value
}
