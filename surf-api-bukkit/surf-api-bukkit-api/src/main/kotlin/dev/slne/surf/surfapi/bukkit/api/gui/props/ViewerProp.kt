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

    fun get(viewer: Player): T = storage.get(viewer.uniqueId)
        ?: throw IllegalStateException("Value for viewer ${viewer.uniqueId} is not set")

    class Mutable<T>(
        override val name: String,
        initialValue: T?
    ) : Prop<T> {
        private val storage = ViewerPropStorage { initialValue }

        override suspend fun get(): T =
            throw UnsupportedOperationException("Use get(viewer: Player) for ViewerProp.MutableViewerProp")

        fun get(viewer: Player): T? = storage.get(viewer.uniqueId)

        fun set(viewer: Player, value: T?) {
            storage.set(viewer.uniqueId, value)
        }

        fun clear(viewer: Player) {
            storage.clear(viewer.uniqueId)
        }
    }
}
