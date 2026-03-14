package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.PaginatedSurfViewContext
import dev.slne.surf.surfapi.core.api.util.toMutableObjectList
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.context.Context

/**
 * Configures pagination for the current [PaginatedSurfViewContext].
 *
 * Builds a [PaginationDslBuilder] from the [block] and stores a factory function in the context
 * that creates the [PaginationStateBuilder] when the view is instantiated. The builder must
 * specify at least one source (via [PaginationDslBuilder.source], [PaginationDslBuilder.computedSource], etc.)
 * and an item factory (via [PaginationDslBuilder.itemFactory] or [PaginationDslBuilder.elementFactory]).
 *
 * ```kotlin
 * paginatedSurfView("Items") {
 *     layoutTarget('I')
 *     pagination<String> {
 *         source { listOf("Item A", "Item B", "Item C") }
 *         itemFactory { name ->
 *             withItem(Material.PAPER) {
 *                 itemMeta = itemMeta?.also { it.displayName(Component.text(name)) }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param T the type of items in the pagination source
 * @receiver the [PaginatedSurfViewContext] for the current view DSL scope
 * @param block configuration block applied to a [PaginationDslBuilder]
 * @throws IllegalStateException if no source is configured in [block]
 * @see PaginationDslBuilder
 */
context(ctx: PaginatedSurfViewContext)
inline fun <T> pagination(block: @InventoryFramworkDSL PaginationDslBuilder<T>.() -> Unit) {
    val builder = PaginationDslBuilder<T>().apply(block)

    val sourceType = requireNotNull(builder.sourceType) {
        "Pagination source must be configured. " +
                "Use source { }, computedSource { }, asyncSource { }, lazySource { }, " +
                "suspendSource { }, etc."
    }

    ctx.paginationStateBuilder = { view ->
        @Suppress("UNCHECKED_CAST")
        val stateBuilder: PaginationStateBuilder<Context, BukkitItemComponentBuilder, T> =
            when (sourceType) {
                is PaginationSourceType.Static ->
                    view.buildPaginationState(sourceType.provider().toMutableObjectList())

                is PaginationSourceType.Computed ->
                    view.buildComputedPaginationState { c ->
                        sourceType.provider(c).toMutableObjectList()
                    }

                is PaginationSourceType.ComputedAsync ->
                    view.buildComputedAsyncPaginationState { c ->
                        sourceType.provider(c).thenApply { it.toMutableObjectList() }
                    }

                is PaginationSourceType.Lazy ->
                    view.buildLazyPaginationState { c ->
                        sourceType.provider(c).toMutableObjectList()
                    }

                is PaginationSourceType.LazyAsync ->
                    view.buildLazyAsyncPaginationState { c ->
                        sourceType.provider(c).thenApply { it.toMutableObjectList() }
                    }
            }

        builder.applyTo(stateBuilder)
        stateBuilder
    }
}
