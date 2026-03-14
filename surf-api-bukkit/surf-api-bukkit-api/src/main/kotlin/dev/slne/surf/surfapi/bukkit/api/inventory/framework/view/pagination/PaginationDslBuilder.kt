package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.InventoryFrameworkDSL
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

/**
 * DSL builder for configuring the data source, item factory, and behaviour of a paginated view.
 *
 * An instance of this class is created by the [pagination] DSL function. You must configure
 * exactly one source method and exactly one item factory, otherwise [applyTo] will throw
 * [IllegalStateException] at view creation time.
 *
 * **Source methods** (choose exactly one):
 * - [source] — static iterable
 * - [computedSource] — context-aware lazy computation
 * - [asyncSource] — async computation returning a [CompletableFuture]
 * - [lazySource] — lazy, context-aware computation (evaluated on first access)
 * - [lazyAsyncSource] — lazy async computation
 * - [suspendSource] / [lazySuspendSource] — Kotlin coroutine-based sources
 *
 * **Item factory methods** (choose exactly one):
 * - [itemFactory] — simple builder lambda receiving only the item value
 * - [elementFactory] — full control over context, index, and value
 *
 * **Optional configuration:**
 * - [onPageSwitch] — called when the page changes
 * - [orientation] / [horizontal] / [vertical] — pagination direction
 *
 * @param T the type of items in the pagination data source
 * @see pagination
 * @see PaginationSourceType
 */
@InventoryFrameworkDSL
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


    /**
     * Sets a static pagination source from a pre-populated [Iterable].
     *
     * @param items the fixed list of items to paginate
     */
    fun source(items: Iterable<T>) {
        sourceType = PaginationSourceType.Static { items }
    }

    /**
     * Sets a static pagination source from a provider lambda evaluated once at creation time.
     *
     * @param provider lambda returning the items to paginate
     */
    fun source(provider: @InventoryFrameworkDSL () -> Iterable<T>) {
        sourceType = PaginationSourceType.Static(provider)
    }

    /**
     * Sets a computed pagination source that is re-evaluated from [Context] on every page load.
     *
     * @param provider lambda receiving the current [Context] and returning items to paginate
     */
    fun computedSource(provider: @InventoryFrameworkDSL (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Computed(provider)
    }

    /**
     * Sets an async computed pagination source. The [provider] returns a [CompletableFuture]
     * that is awaited on every page load.
     *
     * @param provider lambda receiving [Context] and returning a [CompletableFuture] of items
     */
    fun asyncSource(provider: @InventoryFrameworkDSL (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.ComputedAsync(provider)
    }

    /**
     * Sets a lazy pagination source. The [provider] is called only once the first time the
     * page is loaded.
     *
     * @param provider lazy lambda receiving [Context] and returning items
     */
    fun lazySource(provider: @InventoryFrameworkDSL (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Lazy(provider)
    }

    /**
     * Sets a lazy async pagination source. The [provider] is called only on the first page load
     * and returns a [CompletableFuture].
     *
     * @param provider lazy lambda returning a [CompletableFuture] of items
     */
    fun lazyAsyncSource(provider: @InventoryFrameworkDSL (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.LazyAsync(provider)
    }

    /**
     * Sets a suspend-based computed pagination source using a [CoroutineScope].
     *
     * The [provider] is a suspend lambda evaluated on every page load within the given [scope].
     *
     * @param scope the [CoroutineScope] to launch the coroutine in
     * @param provider suspend lambda returning items to paginate
     */
    fun suspendSource(
        scope: CoroutineScope,
        provider: @InventoryFrameworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.ComputedAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    /**
     * Convenience overload of [suspendSource] that uses the [Plugin]'s coroutine scope.
     *
     * @param plugin the [Plugin] whose scope is used to launch the coroutine
     * @param provider suspend lambda returning items to paginate
     */
    fun suspendSource(
        plugin: Plugin,
        provider: @InventoryFrameworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = suspendSource(scope = plugin.scope, provider = provider)

    /**
     * Sets a lazy suspend-based pagination source. The [provider] is evaluated only once
     * on the first page load.
     *
     * @param scope the [CoroutineScope] to launch the coroutine in
     * @param provider suspend lambda returning items to paginate
     */
    fun lazySuspendSource(
        scope: CoroutineScope,
        provider: @InventoryFrameworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.LazyAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    /**
     * Convenience overload of [lazySuspendSource] that uses the [Plugin]'s coroutine scope.
     *
     * @param plugin the [Plugin] whose scope is used to launch the coroutine
     * @param provider suspend lambda returning items to paginate
     */
    fun lazySuspendSource(
        plugin: Plugin,
        provider: @InventoryFrameworkDSL suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = lazySuspendSource(scope = plugin.scope, provider = provider)

    /**
     * Configures a simple item factory that receives a [BukkitItemComponentBuilder] and the
     * item value [T].
     *
     * ```kotlin
     * itemFactory { item ->
     *     withItem(Material.PAPER) {
     *         itemMeta = itemMeta?.also { it.displayName(Component.text(item.name)) }
     *     }
     *     onItemClick { cancel() }
     * }
     * ```
     *
     * @param factory the item builder lambda
     */
    fun itemFactory(factory: @InventoryFrameworkDSL BukkitItemComponentBuilder.(T) -> Unit) {
        this.simpleItemFactory = BiConsumer(factory)
        this.elementFactory = null
    }

    /**
     * Configures a full element factory that receives the [Context], [BukkitItemComponentBuilder],
     * the zero-based item [index], and the item value [T].
     *
     * Use this when you need access to the render context or the item index.
     *
     * @param factory the element builder callback
     */
    fun elementFactory(factory: @InventoryFrameworkDSL (context: Context, builder: BukkitItemComponentBuilder, index: Int, value: T) -> Unit) {
        this.elementFactory = PaginationValueConsumer { context, builder, index, value ->
            factory(context, builder, index, value)
        }
        this.simpleItemFactory = null
    }

    /**
     * Registers a handler that is called each time the page changes.
     *
     * @param handler lambda receiving the [Context] and the new [Pagination] state
     */
    fun onPageSwitch(handler: @InventoryFrameworkDSL (Context, Pagination) -> Unit) {
        this.pageSwitchHandler = BiConsumer(handler)
    }

    /**
     * Sets the pagination [Pagination.Orientation] explicitly.
     *
     * @param orientation the desired [Pagination.Orientation]
     */
    fun orientation(orientation: Pagination.Orientation) {
        this.orientation = orientation
    }

    /**
     * Configures horizontal pagination orientation (items fill left-to-right across rows).
     *
     * @see orientation
     * @see vertical
     */
    fun horizontal() {
        orientation(Pagination.Orientation.HORIZONTAL)
    }

    /**
     * Configures vertical pagination orientation (items fill top-to-bottom down columns).
     *
     * @see orientation
     * @see horizontal
     */
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
                        "Use itemFactory { item -> ... } or elementFactory { ctx, builder, index, item -> ... }"
            )
        }

        pageSwitchHandler?.let { stateBuilder.onPageSwitch(it) }
        orientation?.let { stateBuilder.orientation(it) }
    }
}
