package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import com.github.shynixn.mccoroutine.folia.scope
import dev.slne.surf.surfapi.bukkit.api.builder.buildItem
import dev.slne.surf.surfapi.bukkit.api.builder.displayName
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.open
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.register
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.pagination.AbstractPaginatedSurfView
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.PaginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SimpleViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.PaginatedViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.SimpleViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.paginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.simpleViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.withItem
import dev.slne.surf.surfapi.core.api.util.mutableLongSetOf
import dev.slne.surf.surfapi.core.api.util.toMutableObjectList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.future.future
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.component.BukkitItemComponentBuilder
import me.devnatan.inventoryframework.component.Pagination
import me.devnatan.inventoryframework.component.PaginationStateBuilder
import me.devnatan.inventoryframework.component.PaginationValueConsumer
import me.devnatan.inventoryframework.context.*
import me.devnatan.inventoryframework.state.MutableIntState
import me.devnatan.inventoryframework.state.MutableState
import me.devnatan.inventoryframework.state.State
import org.bukkit.inventory.ItemType
import org.bukkit.plugin.Plugin
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.CompletableFuture
import java.util.function.BiConsumer
import java.util.function.Function
import java.util.function.Supplier

@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class SurfViewDsl

@SurfViewDsl
abstract class AbstractSurfViewContext<ViewRef : AbstractSurfViewRef> @PublishedApi internal constructor() {
    @PublishedApi
    internal val stateRegistry = StateRegistry()

    @PublishedApi
    internal var onInit: (context(ViewRef) ViewConfigBuilder.() -> Unit)? = null

    @PublishedApi
    internal var onUpdate: (context(ViewRef) Context.() -> Unit)? = null

    @PublishedApi
    internal var onOpen: (context(ViewRef) OpenContext.() -> Unit)? = null

    @PublishedApi
    internal var onFirstRender: (context(ViewRef) (RenderContext).() -> Unit)? = null

    @PublishedApi
    internal var onClick: (context(ViewRef) SlotClickContext.() -> Unit)? = null

    @PublishedApi
    internal var onClose: (context(ViewRef) CloseContext.() -> Unit)? = null

    @PublishedApi
    internal var containerDefaults: (context (ViewContainerModificationContext, ViewRef) () -> Unit)? = {}
}

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

@SurfViewDsl
class SurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<SurfViewRef>() {
    @PublishedApi
    internal var settings: SurfViewSettings = SimpleViewSettings()
}

@SurfViewDsl
class PaginatedSurfViewContext @PublishedApi internal constructor() : AbstractSurfViewContext<PaginatedSurfViewRef>() {
    @PublishedApi
    internal var settings: PaginatedViewSettings = PaginatedViewSettings()

    @PublishedApi
    internal var layoutTarget: Char? = null

    @PublishedApi
    internal var paginationStateBuilder: ((AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *>)? =
        null

    fun getRegisteredLayoutTarget(): Char {
        check(layoutTarget != null) {
            "Missing layout target for paginated view. " +
                    "Please specify a layout target using the 'layoutTarget' function in the view configuration."
        }
        return layoutTarget!!
    }

    fun getRegisteredPaginationStateBuilder(): (AbstractPaginatedSurfView) -> PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        check(paginationStateBuilder != null) {
            "Missing pagination configuration for paginated view. " +
                    "Please specify pagination using one of the pagination functions (pagination, computedPagination, etc.) in the view configuration."
        }
        return paginationStateBuilder!!
    }
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onInit(block: context (ViewRef) (@SurfViewDsl ViewConfigBuilder).() -> Unit) {
    ctx.onInit = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onOpen(block: context(ViewRef) (@SurfViewDsl OpenContext).() -> Unit) {
    ctx.onOpen = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onFirstRender(block: context(ViewRef) (@SurfViewDsl RenderContext).() -> Unit) {
    ctx.onFirstRender = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClick(block: context(ViewRef) (@SurfViewDsl SlotClickContext).() -> Unit) {
    ctx.onClick = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClose(block: context(ViewRef) (@SurfViewDsl CloseContext).() -> Unit) {
    ctx.onClose = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onUpdate(
    block: context(ViewRef) (@SurfViewDsl Context).() -> Unit
) {
    ctx.onUpdate = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> containerDefaults(block: context (@SurfViewDsl ViewContainerModificationContext, ViewRef) () -> Unit) {
    ctx.containerDefaults = block
}

context(ctx: SurfViewContext)
fun settings(block: @SurfViewDsl SimpleViewSettingsBuilder.() -> Unit) {
    ctx.settings = simpleViewSettings(block)
}

context(ctx: PaginatedSurfViewContext)
fun settings(block: @SurfViewDsl PaginatedViewSettingsBuilder.() -> Unit) {
    ctx.settings = paginatedViewSettings(block)
}

context(ctx: PaginatedSurfViewContext)
fun layoutTarget(target: Char) {
    ctx.layoutTarget = target
}

@PublishedApi
internal sealed interface PaginationSourceType<T> {
    class Static<T>(val provider: () -> Iterable<T>) : PaginationSourceType<T>
    class Computed<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>
    class ComputedAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>
    class Lazy<T>(val provider: (Context) -> Iterable<T>) : PaginationSourceType<T>
    class LazyAsync<T>(val provider: (Context) -> CompletableFuture<Iterable<T>>) : PaginationSourceType<T>
}

@SurfViewDsl
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

    fun source(provider: @SurfViewDsl () -> Iterable<T>) {
        sourceType = PaginationSourceType.Static(provider)
    }

    fun computedSource(provider: @SurfViewDsl (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Computed(provider)
    }

    fun asyncSource(provider: @SurfViewDsl (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.ComputedAsync(provider)
    }

    fun lazySource(provider: @SurfViewDsl (Context) -> Iterable<T>) {
        sourceType = PaginationSourceType.Lazy(provider)
    }

    fun lazyAsyncSource(provider: @SurfViewDsl (Context) -> CompletableFuture<Iterable<T>>) {
        sourceType = PaginationSourceType.LazyAsync(provider)
    }

    fun suspendSource(
        scope: CoroutineScope,
        provider: @SurfViewDsl suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.ComputedAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    fun suspendSource(
        plugin: Plugin,
        provider: @SurfViewDsl suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = suspendSource(scope = plugin.scope, provider = provider)

    fun lazySuspendSource(
        scope: CoroutineScope,
        provider: @SurfViewDsl suspend CoroutineScope.(Context) -> Iterable<T>,
    ) {
        sourceType = PaginationSourceType.LazyAsync { context ->
            scope.future { provider(context) }.thenApply { it }
        }
    }

    fun lazySuspendSource(
        plugin: Plugin,
        provider: @SurfViewDsl suspend CoroutineScope.(Context) -> Iterable<T>,
    ) = lazySuspendSource(scope = plugin.scope, provider = provider)

    fun itemFactory(factory: @SurfViewDsl BukkitItemComponentBuilder.(T) -> Unit) {
        this.simpleItemFactory = BiConsumer(factory)
        this.elementFactory = null
    }

    fun elementFactory(factory: @SurfViewDsl (context: Context, builder: BukkitItemComponentBuilder, index: Int, value: T) -> Unit) {
        this.elementFactory = PaginationValueConsumer { context, builder, index, value ->
            factory(context, builder, index, value)
        }
        this.simpleItemFactory = null
    }

    fun onPageSwitch(handler: @SurfViewDsl (Context, Pagination) -> Unit) {
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

context(ctx: PaginatedSurfViewContext)
inline fun <T> pagination(block: @SurfViewDsl PaginationDslBuilder<T>.() -> Unit) {
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

context(ref: SurfViewRef)
val view: SurfViewDSLImpl get() = ref.getRegisteredView() as SurfViewDSLImpl

context(ctx: PaginatedSurfViewRef)
val view: AbstractPaginatedSurfView get() = ctx.getRegisteredView() as AbstractPaginatedSurfView

@PublishedApi
internal sealed interface DeferredState<S> {
    class Immutable<T>(val initialValue: T) : DeferredState<State<T>>
    class Mutable<T>(val initialValue: T) : DeferredState<MutableState<T>>
    class MutableInt(val initialValue: Int) : DeferredState<MutableIntState>
    class Computed<T>(val computation: Function<Context, T>) : DeferredState<State<T>>
    class ComputedSupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>
    class Lazy<T>(val computation: Function<Context, T>) : DeferredState<State<T>>
    class LazySupplier<T>(val computation: Supplier<T>) : DeferredState<State<T>>
    class Initial<T>(val key: String?) : DeferredState<MutableState<T>>
}

class StateRegistry @PublishedApi internal constructor() {
    @PublishedApi
    internal val deferredStates = mutableListOf<DeferredState<*>>()

    @PublishedApi
    internal val resolvedStates = mutableListOf<Any>()

    @PublishedApi
    internal var nextIndex = 0

    @PublishedApi
    internal fun <S> register(deferred: DeferredState<S>): Int {
        deferredStates.add(deferred)
        return nextIndex++
    }

    @Suppress("UNCHECKED_CAST")
    fun <S> get(index: Int): S = resolvedStates[index] as S
}

class StateHandle<S> @PublishedApi internal constructor(
    @PublishedApi internal val registry: StateRegistry,
    @PublishedApi internal val index: Int,
) {
    @PublishedApi
    internal fun resolve(): S = registry.get(index)
}

operator fun <T> StateHandle<State<T>>.get(context: Context): T =
    resolve().get(context)

operator fun <T> StateHandle<MutableState<T>>.get(context: Context): T =
    resolve().get(context)

operator fun <T> StateHandle<MutableState<T>>.set(context: Context, value: T) {
    resolve().set(value, context)
}

operator fun StateHandle<MutableIntState>.get(context: Context): Int =
    resolve().get(context)

operator fun StateHandle<MutableIntState>.set(context: Context, value: Int) {
    resolve().set(value, context)
}

fun StateHandle<MutableIntState>.increment(context: Context): Int =
    resolve().increment(context)

fun StateHandle<MutableIntState>.decrement(context: Context): Int =
    resolve().decrement(context)

context(ctx: AbstractSurfViewContext<*>)
fun <T> state(initialValue: T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Immutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> mutableState(initialValue: T): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Mutable(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun mutableState(initialValue: Int): StateHandle<MutableIntState> {
    val index = ctx.stateRegistry.register(DeferredState.MutableInt(initialValue))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Computed(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> computedState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.ComputedSupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: (Context) -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Lazy(Function(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> lazyState(computation: () -> T): StateHandle<State<T>> {
    val index = ctx.stateRegistry.register(DeferredState.LazySupplier(Supplier(computation)))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(null))
    return StateHandle(ctx.stateRegistry, index)
}

context(ctx: AbstractSurfViewContext<*>)
fun <T> initialState(key: String): StateHandle<MutableState<T>> {
    val index = ctx.stateRegistry.register(DeferredState.Initial<T>(key))
    return StateHandle(ctx.stateRegistry, index)
}

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

abstract class SurfViewDSLImpl @PublishedApi internal constructor(
    header: String,
    private val ctx: SurfViewContext,
    private val ref: SurfViewRef,
) : AbstractSurfView(header) {
    override val settings get() = ctx.settings

    init {
        resolveStates(ctx.stateRegistry)
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveStates(registry: StateRegistry) {
        for (deferred in registry.deferredStates) {
            val resolved: Any = when (deferred) {
                is DeferredState.Immutable<*> ->
                    state(deferred.initialValue)

                is DeferredState.Mutable<*> ->
                    mutableState(deferred.initialValue)

                is DeferredState.MutableInt ->
                    mutableState(deferred.initialValue)

                is DeferredState.Computed<*> ->
                    computedState(deferred.computation as Function<Context, Any?>)

                is DeferredState.ComputedSupplier<*> ->
                    computedState(deferred.computation as Supplier<Any?>)

                is DeferredState.Lazy<*> ->
                    lazyState(deferred.computation as Function<Context, Any?>)

                is DeferredState.LazySupplier<*> ->
                    lazyState(deferred.computation as Supplier<Any?>)

                is DeferredState.Initial<*> ->
                    if (deferred.key != null) initialState<Any>(deferred.key)
                    else initialState<Any>()
            }
            registry.resolvedStates.add(resolved)
        }
    }

    override fun onViewInit(config: ViewConfigBuilder) {
        ctx.onInit?.invoke(ref, config)
    }

    override fun onViewUpdate(update: Context) {
        ctx.onUpdate?.invoke(ref, update)
    }

    override fun onViewOpen(open: OpenContext) {
        ctx.onOpen?.invoke(ref, open)
    }

    override fun onViewRender(render: RenderContext) {
        ctx.onFirstRender?.invoke(ref, render)
    }

    override fun onViewClick(click: SlotClickContext) {
        ctx.onClick?.invoke(ref, click)
    }

    override fun onViewClose(close: CloseContext) {
        ctx.onClose?.invoke(ref, close)
    }

    context(modificationCtx: ViewContainerModificationContext)
    override fun containerDefaults() {
        ctx.containerDefaults?.invoke(modificationCtx, ref)
    }

    context(_: SurfViewRef)
    fun modifyContainer(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) {
        modifyContainer0(updateContext, block)
    }

    private fun modifyContainer0(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) = modifyContainer(updateContext, block)
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

abstract class PaginatedSurfViewDSLImpl @PublishedApi internal constructor(
    header: String,
    private val ctx: PaginatedSurfViewContext,
    private val ref: PaginatedSurfViewRef,
) : AbstractPaginatedSurfView(header) {
    override val layoutTarget: Char = ctx.getRegisteredLayoutTarget()
    override val settings get() = ctx.settings

    init {
        resolveStates(ctx.stateRegistry)
    }

    @Suppress("UNCHECKED_CAST")
    private fun resolveStates(registry: StateRegistry) {
        for (deferred in registry.deferredStates) {
            val resolved: Any = when (deferred) {
                is DeferredState.Immutable<*> ->
                    state(deferred.initialValue)

                is DeferredState.Mutable<*> ->
                    mutableState(deferred.initialValue)

                is DeferredState.MutableInt ->
                    mutableState(deferred.initialValue)

                is DeferredState.Computed<*> ->
                    computedState(deferred.computation as Function<Context, Any?>)

                is DeferredState.ComputedSupplier<*> ->
                    computedState(deferred.computation as Supplier<Any?>)

                is DeferredState.Lazy<*> ->
                    lazyState(deferred.computation as Function<Context, Any?>)

                is DeferredState.LazySupplier<*> ->
                    lazyState(deferred.computation as Supplier<Any?>)

                is DeferredState.Initial<*> ->
                    if (deferred.key != null) initialState<Any>(deferred.key)
                    else initialState<Any>()
            }
            registry.resolvedStates.add(resolved)
        }
    }

    override fun createPagination(): PaginationStateBuilder<Context, BukkitItemComponentBuilder, *> {
        return ctx.getRegisteredPaginationStateBuilder()(this)
    }

    override fun onPaginatedInit(config: ViewConfigBuilder) {
        ctx.onInit?.invoke(ref, config)
    }

    override fun onPaginatedUpdate(update: Context) {
        ctx.onUpdate?.invoke(ref, update)
    }

    override fun onPaginatedOpen(open: OpenContext) {
        ctx.onOpen?.invoke(ref, open)
    }

    override fun onPaginatedRender(render: RenderContext) {
        ctx.onFirstRender?.invoke(ref, render)
    }

    override fun onPaginatedClick(click: SlotClickContext) {
        ctx.onClick?.invoke(ref, click)
    }

    override fun onPaginatedClose(close: CloseContext) {
        ctx.onClose?.invoke(ref, close)
    }

    context(modificationCtx: ViewContainerModificationContext)
    override fun applyContainerDefaults() {
        ctx.containerDefaults?.invoke(modificationCtx, ref)
    }

    context(_: PaginatedSurfViewRef)
    fun modifyContainer(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) {
        modifyContainer0(updateContext, block)
    }

    private fun modifyContainer0(
        updateContext: Context? = null,
        block: context(ViewContainerModificationContext) () -> Unit
    ) = modifyContainer(updateContext, block)
}

fun main() {
    val view = paginatedSurfView("test") {
        val counter = mutableState(0)
        val label = state("Hello")
        val playerName = initialState<String>("playerName")

        settings {
            navigateBackOnOutsideClick(false)
        }

        layoutTarget('X')
        pagination {
            lazySuspendSource(JavaPlugin.getProvidingPlugin(javaClass)) {
                delay(1000)
                mutableLongSetOf()
            }

            itemFactory { lng ->
                withItem(ItemType.ARROW) {
                }

                onRender { ctx ->

                }
            }
        }

        containerDefaults {
        }

        onInit {
        }



        onFirstRender {
            val count = counter[this]
            val text = label[this]
            val name = playerName[this]

            slot(1, buildItem(ItemType.DIAMOND) {
                displayName { text("Count: $count") }
            }).onClick { click ->
                counter[click] = count + 1
                counter.increment(click)
            }
        }
    }

    view.open(emptyList())
}