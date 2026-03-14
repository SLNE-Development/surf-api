package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.PaginatedViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.SimpleViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.paginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.simpleViewSettings
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.*

/**
 * Registers the `onInit` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewInit] with both the [ViewRef] and a
 * [ViewConfigBuilder] as context receivers. Use it to configure the view's layout, size,
 * or title programmatically.
 *
 * ```kotlin
 * surfView("My View") {
 *     onInit {
 *         layout {
 *             +"XXXXXXXXX"
 *         }
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the init callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onInit(block: context (ViewRef) (@InventoryFrameworkDSL ViewConfigBuilder).() -> Unit) {
    ctx.onInit = block
}

/**
 * Registers the `onOpen` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewOpen] with both the [ViewRef] and an
 * [OpenContext] as context receivers.
 *
 * ```kotlin
 * surfView("My View") {
 *     onOpen { open ->
 *         if (!player.hasPermission("myPlugin.view")) open.cancel()
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the open callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onOpen(block: context(ViewRef) (@InventoryFrameworkDSL OpenContext).() -> Unit) {
    ctx.onOpen = block
}

/**
 * Registers the `onFirstRender` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewRender] with both the [ViewRef] and a
 * [RenderContext] as context receivers. Use it to place items into the inventory slots.
 *
 * ```kotlin
 * surfView("My View") {
 *     onFirstRender {
 *         slot(4, 2) { withItem(Material.DIAMOND) }
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the render callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onFirstRender(block: context(ViewRef) (@InventoryFrameworkDSL RenderContext).() -> Unit) {
    ctx.onFirstRender = block
}

/**
 * Registers the `onClick` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewClick] with both the [ViewRef] and a
 * [SlotClickContext] as context receivers.
 *
 * ```kotlin
 * surfView("My View") {
 *     onClick { click ->
 *         click.cancel()
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the click callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClick(block: context(ViewRef) (@InventoryFrameworkDSL SlotClickContext).() -> Unit) {
    ctx.onClick = block
}

/**
 * Registers the `onClose` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewClose] with both the [ViewRef] and a
 * [CloseContext] as context receivers.
 *
 * ```kotlin
 * surfView("My View") {
 *     onClose { close ->
 *         if (!allowClose) close.cancel()
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the close callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClose(block: context(ViewRef) (@InventoryFrameworkDSL CloseContext).() -> Unit) {
    ctx.onClose = block
}

/**
 * Registers the `onUpdate` lifecycle callback for this view.
 *
 * The [block] is called during [AbstractSurfView.onViewUpdate] with both the [ViewRef] and a
 * generic [Context] as context receivers. Use it to refresh the view's contents when the view
 * state changes.
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the update callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onUpdate(
    block: context(ViewRef) (@InventoryFrameworkDSL Context).() -> Unit
) {
    ctx.onUpdate = block
}

/**
 * Registers a `containerDefaults` callback that is called during the container setup phase.
 *
 * The [block] is called with a [ViewContainerModificationContext] (for adding/removing
 * container components) and the [ViewRef] as context receivers.
 *
 * ```kotlin
 * surfView("My View") {
 *     containerDefaults {
 *         blockRow(5)  // block the bottom row
 *     }
 * }
 * ```
 *
 * @receiver the [AbstractSurfViewContext] for the current view DSL scope
 * @param block the container defaults callback to register
 */
context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> containerDefaults(block: context (@InventoryFrameworkDSL ViewContainerModificationContext, ViewRef) () -> Unit) {
    ctx.containerDefaults = block
}

/**
 * Configures the [SimpleViewSettings] for this simple (non-paginated) view.
 *
 * The [block] is applied to a [SimpleViewSettingsBuilder] which is then built and stored
 * in the [SurfViewContext].
 *
 * ```kotlin
 * surfView("My View") {
 *     settings {
 *         rows(ViewRows.FOUR)
 *         cancelAllInteractions()
 *     }
 * }
 * ```
 *
 * @receiver the [SurfViewContext] for the current view DSL scope
 * @param block configuration block applied to the [SimpleViewSettingsBuilder]
 */
context(ctx: SurfViewContext)
fun settings(block: @InventoryFrameworkDSL SimpleViewSettingsBuilder.() -> Unit) {
    ctx.settings = simpleViewSettings(block)
}

/**
 * Configures the [PaginatedViewSettings] for this paginated view.
 *
 * The [block] is applied to a [PaginatedViewSettingsBuilder] which is then built and stored
 * in the [PaginatedSurfViewContext].
 *
 * ```kotlin
 * paginatedSurfView("List View") {
 *     settings {
 *         paginationViewRows(PaginationViewRows.THREE)
 *         paginationButtonsAtBottom()
 *     }
 * }
 * ```
 *
 * @receiver the [PaginatedSurfViewContext] for the current view DSL scope
 * @param block configuration block applied to the [PaginatedViewSettingsBuilder]
 */
context(ctx: PaginatedSurfViewContext)
fun settings(block: @InventoryFrameworkDSL PaginatedViewSettingsBuilder.() -> Unit) {
    ctx.settings = paginatedViewSettings(block)
}

/**
 * Sets the layout character that marks pagination item slots.
 *
 * The character must match one used in the inventory layout pattern (set via `onInit { layout { } }`).
 * Slots marked with this character will be populated by the pagination engine.
 *
 * ```kotlin
 * paginatedSurfView("List") {
 *     layoutTarget('I')
 *     onInit { layout { +"IIIIIIIII" } }
 * }
 * ```
 *
 * @receiver the [PaginatedSurfViewContext] for the current view DSL scope
 * @param target the single character identifying pagination item slots in the layout
 */
context(ctx: PaginatedSurfViewContext)
fun layoutTarget(target: Char) {
    ctx.layoutTarget = target
}
