package dev.slne.surf.surfapi.bukkit.api.gui.props

import org.bukkit.entity.Player
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * Base interface for all props in the GUI framework.
 * All props are global to the view and shared across all viewers.
 */
sealed interface Prop<T> {
    /**
     * Gets the value of this prop.
     */
    fun get(): T
    
    /**
     * The name of this prop.
     */
    val name: String
}

/**
 * Viewer-specific prop storage.
 * Maps viewer UUIDs to their prop values.
 */
class ViewerPropStorage<T>(private val initialValue: () -> T) {
    private val storage = mutableMapOf<UUID, T>()
    
    fun get(viewerId: UUID): T = storage.getOrPut(viewerId) { initialValue() }
    
    fun set(viewerId: UUID, value: T) {
        storage[viewerId] = value
    }
    
    fun clear(viewerId: UUID) {
        storage.remove(viewerId)
    }
}

/**
 * Immutable prop - always available and immutable after initialization.
 */
class ImmutableProp<T>(
    override val name: String,
    private val value: T
) : Prop<T> {
    override fun get(): T = value
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}

/**
 * Mutable prop - always available and mutable.
 * Global to the view, shared across all viewers.
 */
class MutableProp<T>(
    override val name: String,
    initialValue: T
) : Prop<T> {
    private val value = AtomicReference(initialValue)
    
    override fun get(): T = value.get()
    
    fun set(value: T) {
        this.value.set(value)
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value.get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value.set(value)
    }
}

/**
 * Viewer-specific mutable prop - isolated per viewer.
 */
class ViewerMutableProp<T>(
    override val name: String,
    initialValue: T
) : Prop<T> {
    private val globalDefault = initialValue
    private val storage = ViewerPropStorage { globalDefault }
    
    override fun get(): T = globalDefault
    
    fun get(viewerId: UUID): T = storage.get(viewerId)
    
    fun set(viewerId: UUID, value: T) {
        storage.set(viewerId, value)
    }
    
    fun clear(viewerId: UUID) {
        storage.clear(viewerId)
    }
}

/**
 * Computed prop - accepts a callback that computes the value.
 */
class ComputedProp<T>(
    override val name: String,
    private val compute: () -> T
) : Prop<T> {
    override fun get(): T = compute()
}

/**
 * Lazy prop - gets available when accessed, using a callback.
 */
class LazyProp<T>(
    override val name: String,
    private val initializer: () -> T,
    private val mutable: Boolean = false
) : Prop<T> {
    private val value = lazy { initializer() }
    private var mutableValue: T? = null
    
    override fun get(): T {
        return mutableValue ?: value.value
    }
    
    fun set(value: T) {
        if (!mutable) {
            throw UnsupportedOperationException("Cannot set immutable lazy prop")
        }
        mutableValue = value
    }
}
