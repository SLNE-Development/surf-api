package dev.slne.surf.surfapi.bukkit.api.gui.props

/**
 * Immutable lazy prop - gets available when accessed, using a callback.
 * Cannot be modified after initialization.
 */
open class LazyProp<T>(
    override val name: String,
    private val initializer: () -> T
) : Prop<T> {
    private val value = lazy { initializer() }

    override suspend fun get(): T = value.value

    override fun toString(): String {
        return "LazyProp(name='$name', initializer=$initializer, value=$value)"
    }

    /**
     * Mutable lazy prop - gets available when accessed, using a callback.
     * Can be modified after initialization.
     */
    open class Mutable<T>(
        override val name: String,
        private val initializer: () -> T?
    ) : Prop<T> {
        private val value = lazy { initializer() }
        private var mutableValue: T? = null

        override suspend fun get(): T? {
            return mutableValue ?: value.value
        }

        fun set(value: T) {
            mutableValue = value
        }

        override fun toString(): String {
            return "Mutable(name='$name', initializer=$initializer, value=$value, mutableValue=$mutableValue)"
        }
    }
}
