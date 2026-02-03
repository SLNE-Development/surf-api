package dev.slne.surf.surfapi.bukkit.api.gui.props

import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.KProperty

/**
 * Mutable prop - always available and mutable.
 * Global to the view, shared across all viewers.
 */
class MutableProp<T>(
    override val name: String,
    initialValue: T
) : Prop<T> {
    private val value = AtomicReference(initialValue)
    
    override suspend fun get(): T = value.get()
    
    fun set(value: T) {
        this.value.set(value)
    }
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value.get()
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value.set(value)
    }
}
