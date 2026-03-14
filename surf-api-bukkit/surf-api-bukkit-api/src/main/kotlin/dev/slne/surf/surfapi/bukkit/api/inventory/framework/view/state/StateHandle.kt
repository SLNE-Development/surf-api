package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State

/**
 * A deferred reference to an IF state object resolved after view construction.
 *
 * A [StateHandle] is returned by the state factory functions in [StateDsl] (e.g. [state],
 * [mutableState], [computedState], etc.). The handle stores an index into the [StateRegistry]
 * and defers resolution until [resolve] is called from a lifecycle callback after the view
 * has been built and `resolveStates` has populated the registry.
 *
 * Operator extensions on [StateHandle] allow concise reading and writing of state values:
 * - `handle[context]` reads the value
 * - `handle[context] = newValue` writes a new value (for mutable states)
 *
 * ```kotlin
 * surfView("Counter") {
 *     val counter = mutableState(0)
 *     onFirstRender {
 *         slot(4) {
 *             withItem(Material.PAPER) { amount = counter[this@onFirstRender] }
 *             onItemClick {
 *                 counter[this] = counter[this] + 1
 *                 update()
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param S the concrete IF state type (e.g. [State], [MutableState], [MutableIntState])
 * @property registry the [StateRegistry] that holds the resolved state objects
 * @property index the index within [registry] used to look up the resolved state
 * @see StateRegistry
 * @see StateDsl
 */
class StateHandle<S> @PublishedApi internal constructor(
    @PublishedApi internal val registry: StateRegistry,
    @PublishedApi internal val index: Int,
) {
    @PublishedApi
    internal fun resolve(): S = registry.get(index)
}

/**
 * Reads the current value of this immutable state from the given [context].
 *
 * @param context the current [Context]
 * @return the state value
 */
operator fun <T> StateHandle<State<T>>.get(context: Context): T =
    resolve().get(context)

/**
 * Reads the current value of this mutable state from the given [context].
 *
 * @param context the current [Context]
 * @return the state value
 */
@JvmName("getMutableState")
operator fun <T> StateHandle<MutableState<T>>.get(context: Context): T =
    resolve().get(context)

/**
 * Sets the value of this mutable state within the given [context].
 *
 * @param context the current [Context]
 * @param value the new value to set
 */
operator fun <T> StateHandle<MutableState<T>>.set(context: Context, value: T) {
    resolve().set(value, context)
}

/**
 * Reads the current integer value of this [MutableIntState] from the given [context].
 *
 * @param context the current [Context]
 * @return the current integer value
 */
operator fun StateHandle<MutableIntState>.get(context: Context): Int =
    resolve().get(context)

/**
 * Sets the integer value of this [MutableIntState] within the given [context].
 *
 * @param context the current [Context]
 * @param value the new integer value
 */
operator fun StateHandle<MutableIntState>.set(context: Context, value: Int) {
    resolve().set(value, context)
}

/**
 * Atomically increments the integer value of this [MutableIntState] by 1.
 *
 * @param context the current [Context]
 * @return the value after incrementing
 */
fun StateHandle<MutableIntState>.increment(context: Context): Int =
    resolve().increment(context)

/**
 * Atomically decrements the integer value of this [MutableIntState] by 1.
 *
 * @param context the current [Context]
 * @return the value after decrementing
 */
fun StateHandle<MutableIntState>.decrement(context: Context): Int =
    resolve().decrement(context)
