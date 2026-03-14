package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.AbstractSurfViewContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.AbstractSurfViewRef
import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State
import java.util.function.Function
import java.util.function.Supplier

context(ctx: AbstractSurfViewContext<*>)
fun <T> state(initialValue: T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Immutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> mutableState(initialValue: T): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Mutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun mutableState(initialValue: Int): StateHandle<MutableIntState> {
    val index = ctx.stateRegistry.register(DeferredState.MutableInt(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Computed(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.ComputedSupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Lazy(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.LazySupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(null))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(key: String): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(key))
    return StateHandle(ctx.stateRegistry, index)
}
