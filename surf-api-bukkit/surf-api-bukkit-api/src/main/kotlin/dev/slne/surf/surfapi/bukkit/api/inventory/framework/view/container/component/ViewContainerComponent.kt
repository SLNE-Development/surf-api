package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component

import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

interface ViewContainerComponent {
    val positionalShift: Int
    val textureWidth: Int

    fun SurfComponentBuilder.renderComponent()

    override fun equals(other: Any?): Boolean
    override fun hashCode(): Int
}