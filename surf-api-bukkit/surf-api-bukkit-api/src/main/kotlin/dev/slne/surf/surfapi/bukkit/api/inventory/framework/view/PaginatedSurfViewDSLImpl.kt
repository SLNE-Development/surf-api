package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.DeferredState
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.StateRegistry
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.*
import java.util.function.Function
import java.util.function.Supplier

abstract class PaginatedSurfViewDSLImpl @PublishedApi internal constructor(
    header: String,
    private val ctx: PaginatedSurfViewContext,
    private val ref: PaginatedSurfViewRef,
) : AbstractPaginatedSurfView(header) {
    override val layoutTarget: Char = ctx.getRegisteredLayoutTarget()
    override val settings get() = ctx.settings

    init {
        resolveStates(ctx.stateRegistry)
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveStates(registry: StateRegistry) {
        for (deferred in registry.deferredStates) {
            val resolved: Any = when (deferred) {
                is DeferredState.Immutable<*> ->
                    state(deferred.initialValue)

                is DeferredState.Mutable<*> ->
                    mutableState(deferred.initialValue)

                is DeferredState.MutableInt ->
                    mutableState(deferred.initialValue)

                is DeferredState.Computed<*> ->
                    computedState(deferred.computation as Function<Context, Any?>)

                is DeferredState.ComputedSupplier<*> ->
                    computedState(deferred.computation as Supplier<Any?>)

                is DeferredState.Lazy<*> ->
                    lazyState(deferred.computation as Function<Context, Any?>)

                is DeferredState.LazySupplier<*> ->
                    lazyState(deferred.computation as Supplier<Any?>)

                is DeferredState.Initial<*> ->
                    if (deferred.key != null) initialState<Any>(deferred.key)
                    else initialState<Any>()
            }
            registry.resolvedStates.add(resolved)
        }
    }

    override fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        return ctx.getRegisteredPaginationStateBuilder()(this)
    }

    override fun onPaginatedInit(config: ViewConfigBuilder) {
        ctx.onInit?.invoke(ref, config)
    }

    override fun onPaginatedUpdate(update: Context) {
        ctx.onUpdate?.invoke(ref, update)
    }

    override fun onPaginatedOpen(open: OpenContext) {
        ctx.onOpen?.invoke(ref, open)
    }

    override fun onPaginatedRender(render: RenderContext) {
        ctx.onFirstRender?.invoke(ref, render)
    }

    override fun onPaginatedClick(click: SlotClickContext) {
        ctx.onClick?.invoke(ref, click)
    }

    override fun onPaginatedClose(close: CloseContext) {
        ctx.onClose?.invoke(ref, close)
    }

    context(modificationCtx: ViewContainerModificationContext)
    override fun applyContainerDefaults() {
        ctx.containerDefaults?.invoke(modificationCtx, ref)
    }

    context(_: PaginatedSurfViewRef)
    fun modifyContainer(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) {
        modifyContainer0(updateContext, block)
    }

    private fun modifyContainer0(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) = modifyContainer(updateContext, block)
}
