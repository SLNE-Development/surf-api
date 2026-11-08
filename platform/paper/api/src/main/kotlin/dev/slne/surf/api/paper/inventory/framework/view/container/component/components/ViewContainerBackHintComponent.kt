package dev.slne.surf.api.paper.inventory.framework.view.container.component.components

import dev.slne.surf.api.core.messages.builder.SurfComponentBuilder
import dev.slne.surf.api.paper.inventory.framework.view.container.component.ViewContainerComponent

@Deprecated("Binary compatibility", level = DeprecationLevel.HIDDEN)
data object ViewContainerBackHintComponent : ViewContainerComponent {
    override val positionalShift = 0
    override val textureWidth = 0

    override fun SurfComponentBuilder.renderComponent() {
    }
}
