package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.dsl

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewBlockCellComponent
import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components.ViewContainerBackHintComponent

context(context: ViewContainerModificationContext)
fun addChild(component: ViewContainerComponent) {
    context.container.addChild(component)
}

context(context: ViewContainerModificationContext)
fun removeChild(component: ViewContainerComponent) {
    context.container.removeChild(component)
}

context(context: ViewContainerModificationContext)
fun <T : ViewContainerComponent> hasComponentOfType(clazz: Class<T>): Boolean {
    return context.container.hasComponentOfType(clazz)
}

context(context: ViewContainerModificationContext)
inline fun <reified T : ViewContainerComponent> hasComponentOfType(): Boolean {
    return context.container.hasComponentOfType<T>()
}

context(context: ViewContainerModificationContext)
fun <T : ViewContainerComponent> removeChildrenOfType(type: Class<T>) {
    context.container.removeChildrenOfType(type)
}

context(context: ViewContainerModificationContext)
inline fun <reified T : ViewContainerComponent> removeChildrenOfType() {
    context.container.removeChildrenOfType<T>()
}

context(context: ViewContainerModificationContext)
fun blockCell(column: Int, row: Int) {
    addChild(ViewBlockCellComponent(column, row))
}

context(context: ViewContainerModificationContext)
fun backHint() {
    addChild(ViewContainerBackHintComponent)
}