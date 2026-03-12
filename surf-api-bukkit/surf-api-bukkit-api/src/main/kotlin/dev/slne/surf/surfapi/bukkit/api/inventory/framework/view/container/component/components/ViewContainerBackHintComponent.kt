package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

data object ViewContainerBackHintComponent : ViewContainerComponent {
    override val positionalShift = -21
    override val textureWidth = 15

    override fun SurfComponentBuilder.renderComponent() {
        text("ꐷ")
        color(Colors.WHITE)
    }
}