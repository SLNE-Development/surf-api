package dev.slne.surf.api.paper.inventory.framework.view.state

import me.devnatan.inventoryframework.context.Context
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State
import java.util.function.Function
import java.util.function.Supplier

/**
 * Internal sealed hierarchy used to defer the creation of IF state objects until a view is
 * fully instantiated.
 *
 * During DSL configuration (inside a [surfView] or [paginatedSurfView] block), the actual
 * view instance has not yet been created. Therefore, state functions in [StateDsl] create
 * [DeferredState] placeholders that are stored in [StateRegistry]. When the view is
 * instantiated, the `resolveStates` method in [SurfViewDSLImpl] / [PaginatedSurfViewDSLImpl]
 * iterates the deferred states and creates the corresponding IF state objects by calling the
 * appropriate factory methods on the [View][me.devnatan.inventoryframework.View].
 *
 * @param S the concrete IF state type that will be created
 * @see StateRegistry
 * @see dev.slne.surf.api.paper.api.inventory.framework.view.state.StateDsl
 */
@PublishedApi
internal sealed interface DeferredState<S> {
    /** Deferred immutable state with a fixed [initialValue]. */
    class Immutable<T>(val initialValue: T) : DeferredState<State<T>>

    /** Deferred mutable state with a fixed [initialValue]. */
    class Mutable<T>(val initialValue: T) : DeferredState<MutableState<T>>

    /** Deferred mutable integer state with a fixed [initialValue]. */
    class MutableInt(val initialValue: Int) : DeferredState<MutableIntState>

    /** Deferred computed state whose value is derived from the [Context] by [computation]. */
    class Computed<T>(val computation: Function<Context, T>) : DeferredState<State<T>>

    /** Deferred computed state whose value is derived from a no-arg [Supplier]. */
    class ComputedSupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>

    /** Deferred lazy state whose value is computed from [Context] on the first access. */
    class Lazy<T>(val computation: Function<Context, T>) : DeferredState<State<T>>

    /** Deferred lazy state whose value is computed from a no-arg [Supplier] on the first access. */
    class LazySupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>

    /**
     * Deferred initial state, optionally identified by a string [key].
     * If [key] is non-null, the state is resolved with the key; otherwise a key-less initial state is created.
     */
    class Initial<T>(val key: String?) : DeferredState<MutableState<T>>
}
