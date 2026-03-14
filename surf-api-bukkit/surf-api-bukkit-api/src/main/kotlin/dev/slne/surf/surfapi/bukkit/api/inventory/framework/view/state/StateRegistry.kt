package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

/**
 * Stores [DeferredState] registrations during DSL configuration and holds the resolved IF
 * state objects after the view is built.
 *
 * The registration flow is:
 * 1. During DSL configuration, state factory calls (e.g. [state], [mutableState]) invoke
 *    [register] which appends a [DeferredState] and returns its index.
 * 2. When the view is instantiated, `resolveStates` iterates [deferredStates], creates the
 *    actual IF states, and appends them to [resolvedStates] in the same order.
 * 3. [StateHandle]s then use [get] to retrieve the resolved state by index.
 *
 * @see DeferredState
 * @see StateHandle
 * @see StateDsl
 */
class StateRegistry @PublishedApi internal constructor() {
    @PublishedApi
    internal val deferredStates = mutableListOf<DeferredState<*>>()

    @PublishedApi
    internal val resolvedStates = mutableListOf<Any>()

    @PublishedApi
    internal var nextIndex = 0

    /**
     * Registers a [deferred] state and returns its allocated index.
     *
     * @param deferred the [DeferredState] to register
     * @return the index that can be used with [get] after states are resolved
     */
    @PublishedApi
    internal fun <S> register(deferred: DeferredState<S>): Int {
        deferredStates.add(deferred)
        return nextIndex++
    }

    /**
     * Returns the resolved state at the given [index].
     *
     * @param S the expected state type
     * @param index the index returned by [register]
     * @return the resolved state cast to [S]
     * @throws IndexOutOfBoundsException if [index] is out of range
     * @throws ClassCastException if the resolved state is not of type [S]
     */
    @Suppress("UNCHECKED_CAST")
    fun <S> get(index: Int): S = resolvedStates[index] as S
}
