package dev.slne.surf.surfapi.bukkit.api.gui.props

import dev.slne.surf.surfapi.core.api.util.mutableObject2ObjectMapOf
import org.bukkit.entity.Player
import java.util.*

/**
 * Viewer-specific prop storage.
 * Maps viewer UUIDs to their prop values.
 */
class ViewerPropStorage<T>(private val initialValue: () -> T) {
    private val storage = mutableObject2ObjectMapOf<UUID, T?>()

    fun get(viewer: Player): T? = storage.getOrPut(viewer.uniqueId) { initialValue() }

    fun set(viewer: Player, value: T?) {
        storage[viewer.uniqueId] = value
    }

    fun clear(viewer: Player) {
        storage.remove(viewer.uniqueId)
    }

    override fun toString(): String {
        return "ViewerPropStorage(initialValue=$initialValue, storage=$storage)"
    }
}
