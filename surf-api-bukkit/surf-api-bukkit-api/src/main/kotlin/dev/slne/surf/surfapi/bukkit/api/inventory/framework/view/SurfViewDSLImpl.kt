package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.DeferredState
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.state.StateRegistry
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.*

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
        ctx.stateRegistry.resolveStates(this)
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
