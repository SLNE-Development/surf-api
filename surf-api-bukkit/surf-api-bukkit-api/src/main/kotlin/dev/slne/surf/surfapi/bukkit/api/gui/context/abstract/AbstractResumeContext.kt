package dev.slne.surf.surfapi.bukkit.api.gui.context.abstract

import dev.slne.surf.surfapi.bukkit.api.gui.context.LifecycleEventType
import dev.slne.surf.surfapi.bukkit.api.gui.context.ResumeContext
import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView
import dev.slne.surf.surfapi.shared.api.util.InternalSurfApi
import org.bukkit.entity.Player

@InternalSurfApi
class AbstractResumeContext(
    override val view: GuiView,
    override val player: Player,
    override val origin: GuiView?
) : ResumeContext {
    override val eventType: LifecycleEventType = LifecycleEventType.RESUME

    override fun navigateTo(view: GuiView, passProps: Boolean) {
        NavigationHelper.navigateTo(this.view, view, player, passProps)
    }

    override fun navigateBack() {
        NavigationHelper.navigateBack(view, player)
    }

    override fun close() {
        NavigationHelper.close(player)
    }

    override fun update() {
        NavigationHelper.update(view, player)
    }

    override fun toString(): String {
        return "AbstractResumeContext(view=$view, player=$player, origin=$origin, eventType=$eventType)"
    }
}
