package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import me.devnatan.inventoryframework.context.Context
import java.util.concurrent.CompletableFuture

/**
 * Sealed hierarchy that describes the strategy used to supply items to a [PaginationDslBuilder].
 *
 * Each variant is an internal implementation detail of the pagination DSL and is not meant
 * to be used directly. Choose the appropriate source via the [PaginationDslBuilder] methods.
 *
 * @param T the type of items in the pagination source
 * @see PaginationDslBuilder
 */
@PublishedApi
internal sealed interface PaginationSourceType<T> {
    /**
     * A static source whose items are provided by a no-argument [provider] lambda.
     * The lambda is called once when the pagination state is first built.
     */
    class Static<T>(val provider: () -> Iterable<T>) : PaginationSourceType<T>

    /**
     * A computed source whose items are derived from the current [Context].
     * Re-evaluated on every page load.
     */
    class Computed<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>

    /**
     * An asynchronous computed source that returns a [CompletableFuture] of items.
     * Re-evaluated on every page load.
     */
    class ComputedAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>

    /**
     * A lazy source whose [provider] is called only on the first page load.
     */
    class Lazy<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>

    /**
     * A lazy asynchronous source whose [provider] is called only on the first page load,
     * returning a [CompletableFuture].
     */
    class LazyAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>
}
