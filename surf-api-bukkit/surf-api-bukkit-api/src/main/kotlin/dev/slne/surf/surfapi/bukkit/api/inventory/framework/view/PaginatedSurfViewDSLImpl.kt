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

/**
 * The concrete implementation of a DSL-configured paginated Surf view.
 *
 * [PaginatedSurfViewDSLImpl] is created by [paginatedSurfView] and wires the lifecycle
 * callbacks from [PaginatedSurfViewContext] to the corresponding hooks in
 * [AbstractPaginatedSurfView]. It also resolves all [DeferredState] entries registered
 * during the DSL block into actual IF state objects at construction time.
 *
 * This class is not intended to be used or subclassed directly; use [paginatedSurfView] instead.
 *
 * @param header the plain-text inventory title
 * @param ctx the [PaginatedSurfViewContext] holding lifecycle callbacks and pagination config
 * @param ref the [PaginatedSurfViewRef] that will be resolved once the view is built
 * @see paginatedSurfView
 * @see AbstractPaginatedSurfView
 * @see PaginatedSurfViewContext
 */
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

    /**
     * Modifies the [ViewContainer] of this paginated view from within a lifecycle callback.
     *
     * Only callable inside a [PaginatedSurfViewRef] context (i.e. within lifecycle callbacks).
     * Delegates to the internal `modifyContainer` method of [AbstractSurfView].
     *
     * @param updateContext optional context used to propagate the updated title;
     *   pass `null` to skip the title update
     * @param block modifications to apply to the [ViewContainer]
     */
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
