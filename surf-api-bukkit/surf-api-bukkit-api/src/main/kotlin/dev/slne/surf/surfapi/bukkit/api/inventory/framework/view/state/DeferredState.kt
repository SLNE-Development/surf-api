package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State
import java.util.function.Function
import java.util.function.Supplier

@PublishedApi
internal sealed interface DeferredState<S> {
    class Immutable<T>(val initialValue: T) : DeferredState<State<T>>
    class Mutable<T>(val initialValue: T) : DeferredState<MutableState<T>>
    class MutableInt(val initialValue: Int) : DeferredState<MutableIntState>
    class Computed<T>(val computation: Function<Context, T>) : DeferredState<State<T>>
    class ComputedSupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>
    class Lazy<T>(val computation: Function<Context, T>) : DeferredState<State<T>>
    class LazySupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>
    class Initial<T>(val key: String?) : DeferredState<MutableState<T>>
}
