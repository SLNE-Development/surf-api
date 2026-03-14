package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView

/**
 * Creates a simple (non-paginated) [AbstractSurfView] using a DSL builder.
 *
 * The [block] is called with a [SurfViewContext] as its context receiver, which exposes
 * lifecycle hooks and view settings for configuration. After the block executes, the
 * concrete view implementation is instantiated and its internal reference is resolved.
 *
 * ```kotlin
 * val myView = surfView("Inventory Title") {
 *     settings { rows(ViewRows.FOUR) }
 *     onFirstRender {
 *         slot(4, 1) { withItem(Material.DIAMOND) }
 *     }
 *     onClick { click ->
 *         click.cancel()
 *     }
 * }
 * myView.register() // Called in JavaPlugin#onLoad
 * myView.open(player)
 * ```
 *
 * @param header the plain-text title rendered in the inventory header
 * @param block DSL configuration block with [SurfViewContext] as its context receiver
 * @return the fully configured [AbstractSurfView]
 * @see paginatedSurfView
 * @see AbstractSurfView
 */
inline fun surfView(header: String, block: context (SurfViewContext) () -> Unit): AbstractSurfView {
    val ctx = SurfViewContext()
    val ref = SurfViewRef()

    context(ctx) {
        block()
    }

    val view = object : SurfViewDSLImpl(header, ctx, ref) {}
    ref.view = view

    return view
}

/**
 * Creates a paginated [AbstractPaginatedSurfView] using a DSL builder.
 *
 * Works similarly to [surfView] but uses a [PaginatedSurfViewContext] as the context
 * receiver. The [block] must configure at least a `layoutTarget` character and a
 * `pagination { }` block, otherwise [IllegalStateException] is thrown at view creation.
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
 * @param block DSL configuration block with [PaginatedSurfViewContext] as its context receiver
 * @return the fully configured [AbstractPaginatedSurfView]
 * @see surfView
 * @see dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView
 */
inline fun paginatedSurfView(
    header: String,
    block: context (PaginatedSurfViewContext) () -> Unit
): AbstractPaginatedSurfView {
    val ctx = PaginatedSurfViewContext()
    val ref = PaginatedSurfViewRef()

    context(ctx) {
        block()
    }

    val view = object : PaginatedSurfViewDSLImpl(header, ctx, ref) {}
    ref.view = view

    return view
}
