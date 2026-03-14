package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFramworkDSL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.future.future
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.component.PaginationValueConsumer
import me.devnatan.inventoryframework.context.Context
import org.bukkit.plugin.Plugin
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer

@InventoryFramworkDSL
class PaginationDslBuilder<T> @PublishedApi internal constructor() {

    @PublishedApi
    internal var sourceType: PaginationSourceType<T>? = null

    @PublishedApi
    internal var elementFactory: PaginationValueConsumer<Context, BukkitItemComponentBuilder, T>? = null

    @PublishedApi
    internal var simpleItemFactory: BiConsumer<BukkitItemComponentBuilder, T>? = null

    @PublishedApi
    internal var pageSwitchHandler: BiConsumer<Context, Pagination>? = null

    @PublishedApi
    internal var orientation: Pagination.Orientation? = null


    fun source(items: Iterable<T>) {
        sourceType = PaginationSourceType.Static { items }
    }

    fun source(provider: @InventoryFramworkDSL () -> Iterable<T>) {
        sourceType = PaginationSourceType.Static(provider)
    }

    fun computedSource(provider: @InventoryFramworkDSL (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Computed(provider)
    }

    fun asyncSource(provider: @InventoryFramworkDSL (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.ComputedAsync(provider)
    }

    fun lazySource(provider: @InventoryFramworkDSL (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Lazy(provider)
    }

    fun lazyAsyncSource(provider: @InventoryFramworkDSL (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.LazyAsync(provider)
    }

    fun suspendSource(
        scope: CoroutineScope,
        provider: @InventoryFramworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.ComputedAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    fun suspendSource(
        plugin: Plugin,
        provider: @InventoryFramworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = suspendSource(scope = plugin.scope, provider = provider)

    fun lazySuspendSource(
        scope: CoroutineScope,
        provider: @InventoryFramworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.LazyAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    fun lazySuspendSource(
        plugin: Plugin,
        provider: @InventoryFramworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = lazySuspendSource(scope = plugin.scope, provider = provider)

    fun itemFactory(factory: @InventoryFramworkDSL BukkitItemComponentBuilder.(T) -> Unit) {
        this.simpleItemFactory = BiConsumer(factory)
        this.elementFactory = null
    }

    fun elementFactory(factory: @InventoryFramworkDSL (context: Context, builder: BukkitItemComponentBuilder, index: Int, value: T) -> Unit) {
        this.elementFactory = PaginationValueConsumer { context, builder, index, value ->
            factory(context, builder, index, value)
        }
        this.simpleItemFactory = null
    }

    fun onPageSwitch(handler: @InventoryFramworkDSL (Context, Pagination) -> Unit) {
        this.pageSwitchHandler = BiConsumer(handler)
    }

    fun orientation(orientation: Pagination.Orientation) {
        this.orientation = orientation
    }

    fun horizontal() {
        orientation(Pagination.Orientation.HORIZONTAL)
    }

    fun vertical() {
        orientation(Pagination.Orientation.VERTICAL)
    }

    @PublishedApi
    internal fun applyTo(stateBuilder: PaginationStateBuilder<Context, BukkitItemComponentBuilder, T>) {
        when {
            elementFactory != null -> stateBuilder.elementFactory(elementFactory!!)
            simpleItemFactory != null -> stateBuilder.itemFactory(simpleItemFactory!!)
            else -> error(
                "Pagination item factory must be configured. " +
                        "Use itemFactory { builder, item -> ... } or elementFactory { ctx, builder, index, item -> ... }"
            )
        }

        pageSwitchHandler?.let { stateBuilder.onPageSwitch(it) }
        orientation?.let { stateBuilder.orientation(it) }
    }
}
