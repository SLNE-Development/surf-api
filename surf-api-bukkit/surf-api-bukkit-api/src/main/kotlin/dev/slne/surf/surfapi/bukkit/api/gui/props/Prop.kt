package dev.slne.surf.surfapi.bukkit.api.gui.props

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * Base interface for all props in the GUI framework.
 * All props are global to the view and shared across all viewers.
 */
sealed interface Prop<T> {
    /**
     * Gets the value of this prop.
     * Suspend function to support ComputedProp with async operations.
     */
    suspend fun get(): T?

    /**
     * The name of this prop.
     */
    val name: String

    /**
     * Immutable prop - always available and immutable after initialization.
     */
    class Immutable<T>(
        override val name: String,
        private val value: T
    ) : Prop<T> {
        override suspend fun get(): T = value

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
    }

    /**
     * Mutable prop - always available and mutable.
     * Global to the view, shared across all viewers.
     */
    open class Mutable<T>(
        override val name: String,
        initialValue: T?
    ) : Prop<T> {
        private val value = AtomicReference<T?>(initialValue)

        override suspend fun get(): T? = value.get()

        fun set(value: T) {
            this.value.set(value)
        }

        operator fun getValue(thisRef: Any?, property: KProperty<*>): T? = value.get()
        operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            this.value.set(value)
        }
    }
}
