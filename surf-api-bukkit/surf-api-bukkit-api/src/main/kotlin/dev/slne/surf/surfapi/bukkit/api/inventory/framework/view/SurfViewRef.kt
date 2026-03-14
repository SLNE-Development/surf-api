package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView

abstract class AbstractSurfViewRef @PublishedApi internal constructor() {
    @PublishedApi
    internal lateinit var view: AbstractSurfView

    fun getRegisteredView(): AbstractSurfView {
        check(::view.isInitialized) {
            "Cannot access view during DSL configuration. " +
                    "Only available inside lifecycle callbacks (onFirstRender, onClose, etc.)"
        }
        return view
    }
}

class SurfViewRef @PublishedApi internal constructor() : AbstractSurfViewRef()
class PaginatedSurfViewRef @PublishedApi internal constructor() : AbstractSurfViewRef()

context(ref: SurfViewRef)
val view: SurfViewDSLImpl get() = ref.getRegisteredView() as SurfViewDSLImpl

context(ctx: PaginatedSurfViewRef)
val view: AbstractPaginatedSurfView get() = ctx.getRegisteredView() as AbstractPaginatedSurfView
