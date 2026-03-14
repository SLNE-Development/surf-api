package dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.components

import dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.container.component.ViewContainerComponent
import dev.slne.surf.surfapi.core.api.messages.Colors
import dev.slne.surf.surfapi.core.api.messages.builder.SurfComponentBuilder

/**
 * A [ViewContainerComponent] that renders a "navigate back" hint glyph in the inventory header.
 *
 * This singleton component is added automatically when
 * [SurfViewSettings.navigateBackOnOutsideClick][dev.slne.surf.surfapi.bukkit.api.inventory.framework.view.settings.SurfViewSettings.navigateBackOnOutsideClick]
 * is `true`. It displays a small arrow icon at a fixed position (shift of −21 pixels,
 * texture width 15 pixels) to hint to the player that clicking outside the inventory navigates back.
 */
data object ViewContainerBackHintComponent : ViewContainerComponent {
    override val positionalShift = -21
    override val textureWidth = 15

    override fun SurfComponentBuilder.renderComponent() {
        text("ꐷ")
        color(Colors.WHITE)
    }
}