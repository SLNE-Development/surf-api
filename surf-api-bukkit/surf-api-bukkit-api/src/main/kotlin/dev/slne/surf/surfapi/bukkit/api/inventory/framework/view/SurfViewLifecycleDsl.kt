package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl.ViewContainerModificationContext
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.PaginatedViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.SimpleViewSettingsBuilder
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.paginatedViewSettings
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.builder.simpleViewSettings
import me.devnatan.inventoryframework.ViewConfigBuilder
import me.devnatan.inventoryframework.context.*

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onInit(block: context (ViewRef) (@InventoryFramworkDSL ViewConfigBuilder).() -> Unit) {
    ctx.onInit = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onOpen(block: context(ViewRef) (@InventoryFramworkDSL OpenContext).() -> Unit) {
    ctx.onOpen = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onFirstRender(block: context(ViewRef) (@InventoryFramworkDSL RenderContext).() -> Unit) {
    ctx.onFirstRender = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClick(block: context(ViewRef) (@InventoryFramworkDSL SlotClickContext).() -> Unit) {
    ctx.onClick = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onClose(block: context(ViewRef) (@InventoryFramworkDSL CloseContext).() -> Unit) {
    ctx.onClose = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> onUpdate(
    block: context(ViewRef) (@InventoryFramworkDSL Context).() -> Unit
) {
    ctx.onUpdate = block
}

context(ctx: AbstractSurfViewContext<ViewRef>)
fun <ViewRef : AbstractSurfViewRef> containerDefaults(block: context (@InventoryFramworkDSL ViewContainerModificationContext, ViewRef) () -> Unit) {
    ctx.containerDefaults = block
}

context(ctx: SurfViewContext)
fun settings(block: @InventoryFramworkDSL SimpleViewSettingsBuilder.() -> Unit) {
    ctx.settings = simpleViewSettings(block)
}

context(ctx: PaginatedSurfViewContext)
fun settings(block: @InventoryFramworkDSL PaginatedViewSettingsBuilder.() -> Unit) {
    ctx.settings = paginatedViewSettings(block)
}

context(ctx: PaginatedSurfViewContext)
fun layoutTarget(target: Char) {
    ctx.layoutTarget = target
}
