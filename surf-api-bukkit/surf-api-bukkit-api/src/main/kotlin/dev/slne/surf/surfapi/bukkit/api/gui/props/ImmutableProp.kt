package dev.slne.surf.surfapi.bukkit.api.gui.props

import kotlin.reflect.KProperty

/**
 * Immutable prop - always available and immutable after initialization.
 */
class ImmutableProp<T>(
    override val name: String,
    private val value: T
) : Prop<T> {
    override suspend fun get(): T = value
    
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = value
}
