package dev.slne.surf.surfapi.bukkit.api.gui.props

import java.util.UUID

/**
 * Viewer-specific mutable prop - isolated per viewer.
 */
class ViewerMutableProp<T>(
    override val name: String,
    initialValue: T
) : Prop<T> {
    private val globalDefault = initialValue
    private val storage = ViewerPropStorage { globalDefault }
    
    override suspend fun get(): T = globalDefault
    
    fun get(viewerId: UUID): T = storage.get(viewerId)
    
    fun set(viewerId: UUID, value: T) {
        storage.set(viewerId, value)
    }
    
    fun clear(viewerId: UUID) {
        storage.clear(viewerId)
    }
}
