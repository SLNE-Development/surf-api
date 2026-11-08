package dev.slne.surf.api.minestom.inventory.framework.view.button

import dev.slne.surf.api.core.inventory.framework.internal.ViewButtonSession
import dev.slne.surf.api.minestom.inventory.framework.view.state.StateHandle
import dev.slne.surf.api.minestom.inventory.framework.view.state.get
import me.devnatan.inventoryframework.component.MinestomItemComponentBuilder
import me.devnatan.inventoryframework.context.CloseContext
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.context.SlotClickContext
import me.devnatan.inventoryframework.context.SlotRenderContext
import me.devnatan.inventoryframework.state.State

/**
 * A handle to a stateful button declared in a view's DSL block.
 *
 * The handle is returned by [statefulButton], [toggleButton] and [tripleButton] and is what
 * connects the button's configuration to an actual inventory slot: place it with
 * [MinestomItemComponentBuilder.button] inside a render callback.
 *
 * The button's value is stored per view session, so every viewer cycles their own copy. Read and
 * write it from any [Context] with the indexed operators:
 *
 * ```kotlin
 * val sound = toggleButton(initial = true) { ... }
 *
 * onFirstRender {
 *     slot(1, 1) { button(sound) }
 *     slot(1, 3) {
 *         withItem(Material.PAPER)
 *         onItemClick { player.sendMessage("Sound ist ${sound[this]}") }
 *     }
 * }
 * ```
 *
 * @param T the type identifying the button's states
 * @see statefulButton
 * @see toggleButton
 * @see tripleButton
 */
class ViewButtonHandle<T> internal constructor(
    private val sessionState: StateHandle<State<ViewButtonSession<T>>>,
    private val spec: ViewButtonSpec<T>,
) {
    private fun session(context: Context): ViewButtonSession<T> = sessionState[context]

    /**
     * Reads the state this button currently shows for [context]'s viewer.
     *
     * @param context the current [Context]
     * @return the currently shown state
     */
    operator fun get(context: Context): T = session(context).current

    /**
     * Jumps this button to [value] for [context]'s viewer.
     *
     * Neither the per-state [ViewButtonStateScope.onEnter] nor [ViewButtonBuilder.onChange]
     * callbacks fire — those are reserved for actual clicks — and the slot is not re-rendered.
     * Call [Context.update][me.devnatan.inventoryframework.context.IFContext.update] afterwards to
     * make the change visible.
     *
     * @param context the current [Context]
     * @param value the state to jump to; must be one of the button's declared states
     * @throws IllegalArgumentException if [value] is not a declared state
     */
    operator fun set(context: Context, value: T) {
        session(context).select(value)
    }

    /**
     * Returns the state this button started out with for [context]'s viewer.
     *
     * @param context the current [Context]
     * @return the initial state
     */
    fun initial(context: Context): T = session(context).initial

    /**
     * Returns whether this button currently shows a different state than the one it started out
     * with for [context]'s viewer.
     *
     * @param context the current [Context]
     * @return `true` if the button was used to move to another state
     */
    fun hasChanged(context: Context): Boolean = session(context).hasChanged

    internal fun renderInto(context: SlotRenderContext) {
        val session = session(context)
        context.item = spec.states[session.currentIndex].itemFactory(context)
    }

    internal fun handleClick(context: SlotClickContext) {
        val session = session(context)
        val step = if (spec.reverseOnRightClick && context.isRightClick) -1 else 1

        val from = session.current
        if (!session.advance(step)) return
        val to = session.current

        // Repaint before the callbacks run: they are allowed to navigate away, which would leave
        // nothing sensible to update afterwards.
        context.component?.update()

        spec.states[session.currentIndex].enterHandler?.invoke(context, from)
        spec.changeHandler?.invoke(context, from, to)
    }

    internal fun handleClose(context: CloseContext) {
        val handler = spec.closeChangedHandler ?: return
        val session = session(context)

        if (!session.hasChanged) return

        handler(context, session.initial, session.current)
    }
}

/**
 * Places [handle]'s button in this slot.
 *
 * Wires up the three things a stateful button needs: it renders the appearance of the currently
 * shown state, cycles to the next state on click, and cancels the click so the item cannot be
 * picked up.
 *
 * ```kotlin
 * onFirstRender {
 *     slot(1, 1) { button(sound) }
 *     slot(1, 3) { button(difficulty) }
 * }
 * ```
 *
 * @receiver the [MinestomItemComponentBuilder] of the slot the button should occupy
 * @param handle the button declared in the view's DSL block
 * @return this builder for chaining
 */
fun <T> MinestomItemComponentBuilder.button(handle: ViewButtonHandle<T>): MinestomItemComponentBuilder {
    cancelOnClick()
    onRender { context -> handle.renderInto(context) }
    onClick { context -> handle.handleClick(context) }

    return this
}
