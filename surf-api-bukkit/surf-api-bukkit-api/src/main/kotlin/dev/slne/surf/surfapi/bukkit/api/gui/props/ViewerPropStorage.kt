package dev.slne.surf.surfapi.bukkit.api.gui.props

import java.util.*

/**
 * Viewer-specific prop storage.
 * Maps viewer UUIDs to their prop values.
 */
class ViewerPropStorage<T>(private val initialValue: () -> T) {
    private val storage = mutableMapOf<UUID, T?>()

    fun get(viewerId: UUID): T? = storage.getOrPut(viewerId) { initialValue() }

    fun set(viewerId: UUID, value: T?) {
        storage[viewerId] = value
    }

    fun clear(viewerId: UUID) {
        storage.remove(viewerId)
    }

    override fun toString(): String {
        return "ViewerPropStorage(initialValue=$initialValue, storage=$storage)"
    }
}
