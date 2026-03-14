package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.register
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView

inline fun surfView(header: String, block: context (SurfViewContext, SurfViewRef) () -> Unit): AbstractSurfView {
    val ctx = SurfViewContext()
    val ref = SurfViewRef()

    context(ctx, ref) {
        block()
    }

    val view = object : SurfViewDSLImpl(header, ctx, ref) {}
    ref.view = view

    view.register()
    return view
}

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

    view.register()
    return view
}
