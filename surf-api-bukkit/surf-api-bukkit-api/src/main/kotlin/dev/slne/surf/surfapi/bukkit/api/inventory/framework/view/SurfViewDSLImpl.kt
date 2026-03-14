package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.DeferredState
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.StateRegistry
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.*
import java.util.function.Function
import java.util.function.Supplier

/**
 * The concrete implementation of a DSL-configured simple Surf view.
 *
 * [SurfViewDSLImpl] is the object created by [surfView] to combine the lifecycle callbacks
 * stored in [SurfViewContext] with the state management from [StateRegistry].
 *
 * During construction, all [DeferredState] entries registered in the DSL configuration block
 * are resolved into actual IF state objects ([State], [MutableState], etc.) by calling the
 * corresponding `state()` / `mutableState()` / `computedState()` factory methods inherited
 * from [View][me.devnatan.inventoryframework.View].
 *
 * This class is not meant to be used or subclassed directly; use [surfView] instead.
 *
 * @param header the plain-text inventory title
 * @param ctx the [SurfViewContext] holding all registered lifecycle callbacks
 * @param ref the [SurfViewRef] that will be resolved once the view instance is created
 * @see surfView
 * @see SurfViewContext
 * @see SurfViewRef
 */
abstract class SurfViewDSLImpl @PublishedApi internal constructor(
    header: String,
    private val ctx: SurfViewContext,
    private val ref: SurfViewRef,
) : AbstractSurfView(header) {
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

    override fun onViewInit(config: ViewConfigBuilder) {
        ctx.onInit?.invoke(ref, config)
    }

    override fun onViewUpdate(update: Context) {
        ctx.onUpdate?.invoke(ref, update)
    }

    override fun onViewOpen(open: OpenContext) {
        ctx.onOpen?.invoke(ref, open)
    }

    override fun onViewRender(render: RenderContext) {
        ctx.onFirstRender?.invoke(ref, render)
    }

    override fun onViewClick(click: SlotClickContext) {
        ctx.onClick?.invoke(ref, click)
    }

    override fun onViewClose(close: CloseContext) {
        ctx.onClose?.invoke(ref, close)
    }

    context(modificationCtx: ViewContainerModificationContext)
    override fun containerDefaults() {
        ctx.containerDefaults?.invoke(modificationCtx, ref)
    }

    /**
     * Modifies the [ViewContainer] of this view from within a lifecycle callback.
     *
     * This function is only callable inside a [SurfViewRef] context (i.e. within lifecycle
     * callbacks). It is a type-safe forwarding wrapper that delegates to the internal
     * `modifyContainer` method of [AbstractSurfView].
     *
     * ```kotlin
     * onFirstRender {
     *     with(view) {
     *         modifyContainer {
     *             blockRow(5)
     *         }
     *     }
     * }
     * ```
     *
     * @param updateContext optional context used to propagate the updated title;
     *   pass `null` to skip the title update
     * @param block modifications to apply to the [ViewContainer]
     */
    context(_: SurfViewRef)
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
