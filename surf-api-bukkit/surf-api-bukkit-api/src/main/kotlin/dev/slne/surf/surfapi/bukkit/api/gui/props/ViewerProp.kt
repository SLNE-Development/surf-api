package dev.slne.surf.surfapi.bukkit.api.gui.props

import org.bukkit.entity.Player

/**
 * Viewer-specific mutable prop - isolated per viewer.
 */
open class ViewerProp<T>(
    override val name: String,
    initialValue: T
) : Prop<T> {
    private val storage = ViewerPropStorage { initialValue }

    override suspend fun get(): T =
        throw UnsupportedOperationException("Use get(viewer: Player) for ViewerProp")

    fun get(viewer: Player): T = storage.get(viewer)
        ?: throw IllegalStateException("Value for viewer ${viewer.uniqueId} is not set")

    fun clear(viewer: Player) {
        storage.clear(viewer)
    }

    override fun toString(): String {
        return "ViewerProp(name='$name', storage=$storage)"
    }

    class Mutable<T>(
        override val name: String,
        initialValue: T?
    ) : Prop<T> {
        private val storage = ViewerPropStorage { initialValue }

        override suspend fun get(): T =
            throw UnsupportedOperationException("Use get(viewer: Player) for ViewerProp.MutableViewerProp")

        fun get(viewer: Player): T? = storage.get(viewer)

        fun getOrDefault(viewer: Player, defaultValue: T): T {
            return storage.get(viewer) ?: defaultValue
        }

        fun set(viewer: Player, value: T?) {
            storage.set(viewer, value)
        }

        fun clear(viewer: Player) {
            storage.clear(viewer)
        }

        override fun toString(): String {
            return "Mutable(name='$name', storage=$storage)"
        }
    }
}
