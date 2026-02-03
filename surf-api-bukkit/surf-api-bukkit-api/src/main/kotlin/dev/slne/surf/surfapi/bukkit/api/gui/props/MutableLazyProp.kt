package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Mutable lazy prop - gets available when accessed, using a callback.
 * Can be modified after initialization.
 */
class MutableLazyProp<T>(
    override val name: String,
    private val initializer: () -> T
) : Prop<T> {
    private val value = lazy { initializer() }
    private var mutableValue: T? = null
    
    override suspend fun get(): T {
        return mutableValue ?: value.value
    }
    
    fun set(value: T) {
        mutableValue = value
    }
}
