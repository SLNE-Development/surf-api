package dev.slne.surf.api.paper.inventory.framework.view

import dev.slne.surf.api.paper.inventory.framework.view.pagination.AbstractPaginatedSurfView

/**
 * A deferred reference to an [AbstractSurfView] that is resolved after the DSL builder
 * function finishes constructing the view instance.
 *
 * During the DSL configuration block (e.g. inside [surfView] or [paginatedSurfView]), the
 * [view] property is not yet accessible. Only within lifecycle callbacks (such as
 * `onFirstRender`, `onOpen`, etc.) is the view fully constructed and available via
 * [getRegisteredView].
 *
 * Subclasses ([SurfViewRef] and [PaginatedSurfViewRef]) act as typed tokens that allow the
 * context-receiver extensions `val view` to return the correct concrete type.
 *
 * @see SurfViewRef
 * @see PaginatedSurfViewRef
 */
abstract class AbstractSurfViewRef @PublishedApi internal constructor() {
    @PublishedApi
    internal lateinit var view: AbstractSurfView

    /**
     * Returns the fully constructed [AbstractSurfView] associated with this reference.
     *
     * @throws IllegalStateException if called before the view is built (i.e. during DSL configuration)
     */
    fun getRegisteredView(): AbstractSurfView {
        check(::view.isInitialized) {
            "Cannot access view during DSL configuration. " +
                    "Only available inside lifecycle callbacks (onFirstRender, onClose, etc.)"
        }
        return view
    }
}

/**
 * Typed reference token for a simple (non-paginated) Surf view.
 *
 * Used as a context receiver in lifecycle callbacks so that the `view` extension
 * property resolves to [SurfViewDSLImpl].
 *
 * @see view
 */
class SurfViewRef @PublishedApi internal constructor() : AbstractSurfViewRef()

/**
 * Typed reference token for a paginated Surf view.
 *
 * Used as a context receiver in lifecycle callbacks so that the `view` extension
 * property resolves to [AbstractPaginatedSurfView].
 *
 * @see view
 */
class PaginatedSurfViewRef @PublishedApi internal constructor() : AbstractSurfViewRef()

/**
 * Returns the [SurfViewDSLImpl] instance associated with this [SurfViewRef].
 *
 * Only available inside lifecycle callbacks; accessing this property during DSL
 * configuration will throw [IllegalStateException].
 *
 * @receiver the [SurfViewRef] for the current DSL scope
 * @throws IllegalStateException if accessed before the view is built
 */
context(ref: SurfViewRef)
val view: SurfViewDSLImpl get() = ref.getRegisteredView() as SurfViewDSLImpl

/**
 * Returns the [AbstractPaginatedSurfView] instance associated with this [PaginatedSurfViewRef].
 *
 * Only available inside lifecycle callbacks; accessing this property during DSL
 * configuration will throw [IllegalStateException].
 *
 * @receiver the [PaginatedSurfViewRef] for the current DSL scope
 * @throws IllegalStateException if accessed before the view is built
 */
context(ctx: PaginatedSurfViewRef)
val view: AbstractPaginatedSurfView get() = ctx.getRegisteredView() as AbstractPaginatedSurfView
