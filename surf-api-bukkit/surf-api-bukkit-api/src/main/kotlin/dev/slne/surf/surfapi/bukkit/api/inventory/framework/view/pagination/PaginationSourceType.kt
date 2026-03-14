package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import me.devnatan.inventoryframework.context.Context
import java.util.concurrent.CompletableFuture

@PublishedApi
internal sealed interface PaginationSourceType<T> {
    class Static<T>(val provider: () -> Iterable<T>) : PaginationSourceType<T>
    class Computed<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>
    class ComputedAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>
    class Lazy<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>
    class LazyAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>
}
