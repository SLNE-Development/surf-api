package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State

class StateHandle<S> @PublishedApi internal constructor(
    @PublishedApi internal val registry: StateRegistry,
    @PublishedApi internal val index: Int,
) {
    @PublishedApi
    internal fun resolve(): S = registry.get(index)
}

operator fun <T> StateHandle<State<T>>.get(context: Context): T =
    resolve().get(context)

@JvmName("getMutableState")
operator fun <T> StateHandle<MutableState<T>>.get(context: Context): T =
    resolve().get(context)

operator fun <T> StateHandle<MutableState<T>>.set(context: Context, value: T) {
    resolve().set(value, context)
}

operator fun StateHandle<MutableIntState>.get(context: Context): Int =
    resolve().get(context)

operator fun StateHandle<MutableIntState>.set(context: Context, value: Int) {
    resolve().set(value, context)
}

fun StateHandle<MutableIntState>.increment(context: Context): Int =
    resolve().increment(context)

fun StateHandle<MutableIntState>.decrement(context: Context): Int =
    resolve().decrement(context)
