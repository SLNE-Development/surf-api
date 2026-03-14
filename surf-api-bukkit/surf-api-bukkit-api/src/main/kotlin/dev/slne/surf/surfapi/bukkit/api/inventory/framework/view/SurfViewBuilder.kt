package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView

/**
 * Creates a simple (non-paginated) [AbstractSurfView] using a DSL builder.
 *
 * The [block] is called with both a [SurfViewContext] (for lifecycle hooks and settings)
 * and a [SurfViewRef] (for accessing the view inside callbacks) as context receivers.
 * After the block executes, the concrete view implementation is instantiated and the
 * reference is resolved.
 *
 * ```kotlin
 * val myView = surfView("Inventory Title") {
 *     settings { rows(ViewRows.FOUR) }
 *     onFirstRender {
 *         slot(4, 1) { withItem(Material.DIAMOND) }
 *     }
 *     onViewClick { click ->
 *         click.cancel()
 *     }
 * }
 * myView.register()
 * myView.open(player)
 * ```
 *
 * @param header the plain-text title rendered in the inventory header
 * @param block DSL configuration block accepting both [SurfViewContext] and [SurfViewRef]
 * @return the fully configured [AbstractSurfView]
 * @see paginatedSurfView
 * @see AbstractSurfView
 */
inline fun surfView(header: String, block: context (SurfViewContext, SurfViewRef) () -> Unit): AbstractSurfView {
    val ctx = SurfViewContext()
    val ref = SurfViewRef()

    context(ctx, ref) {
        block()
    }

    val view = object : SurfViewDSLImpl(header, ctx, ref) {}
    ref.view = view

    return view
}

/**
 * Creates a paginated [AbstractPaginatedSurfView] using a DSL builder.
 *
 * Works the same as [surfView] but accepts a [PaginatedSurfViewContext] and
 * [PaginatedSurfViewRef]. The [block] must configure at least a `layoutTarget` character
 * and a `pagination { }` block, otherwise [IllegalStateException] is thrown at view creation.
 *
 * ```kotlin
 * val listView = paginatedSurfView("Item List") {
 *     settings { paginationViewRows(PaginationViewRows.THREE) }
 *     layoutTarget('I')
 *     pagination<MyItem> {
 *         source { myItemRepository.findAll() }
 *         itemFactory { item ->
 *             withItem(item.material)
 *             onItemClick { cancel() }
 *         }
 *     }
 * }
 * listView.register()
 * listView.open(player)
 * ```
 *
 * @param header the plain-text title rendered in the inventory header
 * @param block DSL configuration block accepting both [PaginatedSurfViewContext] and [PaginatedSurfViewRef]
 * @return the fully configured [AbstractPaginatedSurfView]
 * @see surfView
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView
 */
inline fun paginatedSurfView(
    header: String,
    block: context (PaginatedSurfViewContext, PaginatedSurfViewRef) () -> Unit
): AbstractPaginatedSurfView {
    val ctx = PaginatedSurfViewContext()
    val ref = PaginatedSurfViewRef()

    context(ctx, ref) {
        block()
    }

    val view = object : PaginatedSurfViewDSLImpl(header, ctx, ref) {}
    ref.view = view

    return view
}
