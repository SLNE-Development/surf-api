package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state

class StateRegistry @PublishedApi internal constructor() {
    @PublishedApi
    internal val deferredStates = mutableListOf<DeferredState<*>>()

    @PublishedApi
    internal val resolvedStates = mutableListOf<Any>()

    @PublishedApi
    internal var nextIndex = 0

    @PublishedApi
    internal fun <S> register(deferred: DeferredState<S>): Int {
        deferredStates.add(deferred)
        return nextIndex++
    }

    @Suppress("UNCHECKED_CAST")
    fun <S> get(index: Int): S = resolvedStates[index] as S
}
