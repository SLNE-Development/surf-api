package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.AbstractSurfViewContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.AbstractSurfViewRef
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State
import java.util.function.Function
import java.util.function.Supplier

/**
 * Creates a deferred immutable [State] with the given [initialValue].
 *
 * The state is not created immediately; instead a [DeferredState.Immutable] placeholder is
 * registered in the [StateRegistry]. The actual IF state is resolved when the view is built.
 *
 * ```kotlin
 * surfView("My View") {
 *     val counter = state(0)
 *     onFirstRender {
 *         val value = counter.value.get(this)
 *         // ...
 *     }
 * }
 * ```
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initialValue the initial value of the state
 * @return a [StateHandle] that resolves to the [State] at view instantiation time
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> state(initialValue: T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Immutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred mutable [MutableState] with the given [initialValue].
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initialValue the initial value of the mutable state
 * @return a [StateHandle] that resolves to a [MutableState]
 * @see state
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> mutableState(initialValue: T): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Mutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred mutable integer state ([MutableIntState]) with the given [initialValue].
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param initialValue the initial integer value
 * @return a [StateHandle] that resolves to a [MutableIntState]
 * @see mutableState
 */
context(ctx: AbstractSurfViewContext<*>)
fun mutableState(initialValue: Int): StateHandle<MutableIntState> {
    val index = ctx.stateRegistry.register(DeferredState.MutableInt(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred computed [State] whose value is derived from the [Context] by [computation].
 *
 * The computation is re-evaluated every time the state is accessed within a context.
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param computation function receiving the current [Context] and returning the state value
 * @return a [StateHandle] that resolves to a computed [State]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Computed(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred computed [State] whose value is derived from a no-argument [computation].
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param computation no-argument supplier returning the state value
 * @return a [StateHandle] that resolves to a computed [State]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.ComputedSupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred lazy [State] whose value is computed from [Context] only on the first access.
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param computation function receiving [Context] and returning the lazily computed value
 * @return a [StateHandle] that resolves to a lazy [State]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Lazy(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred lazy [State] whose value is computed from a no-argument [computation] on the first access.
 *
 * @param T the type of the state value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param computation no-argument supplier returning the lazily computed value
 * @return a [StateHandle] that resolves to a lazy [State]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.LazySupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred initial [MutableState] without a named key.
 *
 * Initial states receive their value from the data passed when the view is opened
 * (e.g. via `view.open(player, myData)`). The state type must match the type of the
 * passed data.
 *
 * @param T the type of the initial value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @return a [StateHandle] that resolves to an initial [MutableState]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(null))
    return StateHandle(ctx.stateRegistry, index)
}

/**
 * Creates a deferred initial [MutableState] identified by the given string [key].
 *
 * When the view is opened with a `Map<String, Any>` as initial data, this state
 * automatically receives the value associated with [key] from that map.
 *
 * @param T the type of the initial value
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param key the string key to look up in the initial data map
 * @return a [StateHandle] that resolves to an initial [MutableState] with key [key]
 */
context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(key: String): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(key))
    return StateHandle(ctx.stateRegistry, index)
}
