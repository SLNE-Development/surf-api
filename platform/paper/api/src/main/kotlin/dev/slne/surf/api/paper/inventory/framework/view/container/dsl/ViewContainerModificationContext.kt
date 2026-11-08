package dev.slne.surf.api.paper.inventory.framework.view.container.dsl

import dev.slne.surf.api.paper.inventory.framework.view.container.ViewContainer

/**
 * DSL context that exposes the [ViewContainer] modification API.
 *
 * An instance of this class is passed as a context receiver to every block that modifies
 * the view's container (e.g. `modifyContainer { }`, `containerDefaults { }`). All top-level
 * functions in `ViewContainerDSL.kt` use this as their context receiver to delegate to the
 * underlying [ViewContainer].
 *
 * @property container the [ViewContainer] being modified
 * @see addChild
 * @see removeChild
 * @see blockCell
 */
@ConsistentCopyVisibility
data class ViewContainerModificationContext internal constructor(@PublishedApi internal val container: ViewContainer)