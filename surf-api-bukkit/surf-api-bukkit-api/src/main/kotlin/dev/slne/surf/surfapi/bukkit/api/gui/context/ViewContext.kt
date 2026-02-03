package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.props.Prop
import dev.slne.surf.surfapi.bukkit.api.gui.props.ViewerProp
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import org.bukkit.entity.Player

/**
 * Context representing a snapshot of the current GUI state and props.
 * This is passed to all user action handlers and provides access to view, props, and player.
 */
interface ViewContext {
    /**
     * The view this context belongs to.
     */
    val view: GuiView

    /**
     * The player interacting with the GUI.
     */
    val player: Player

    /**
     * Get a prop value from the view.
     */
    suspend fun <T> getProp(prop: Prop<T>): T? = prop.get()

    /**
     * Get a prop value for a specific player.
     */
    fun <T> getPropForPlayer(prop: Prop<T>, player: Player): T {
        return when (prop) {
            is ViewerProp -> prop.get(player)
            else -> throw UnsupportedOperationException()
        }
    }

    /**
     * Navigate to another view.
     */
    fun navigateTo(view: GuiView, passProps: Boolean = false)

    /**
     * Navigate back to parent view.
     */
    fun navigateBack()

    /**
     * Close the GUI.
     */
    fun close()

    /**
     * Update the current view.
     */
    fun update()
}
