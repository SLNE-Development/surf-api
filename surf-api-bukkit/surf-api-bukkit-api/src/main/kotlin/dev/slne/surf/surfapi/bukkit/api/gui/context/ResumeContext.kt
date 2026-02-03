package dev.slne.surf.surfapi.bukkit.api.gui.context

import dev.slne.surf.surfapi.bukkit.api.gui.view.GuiView

/**
 * Context for resume events (navigation back).
 */
interface ResumeContext : LifecycleContext {
    /**
     * The view we're navigating from (origin).
     */
    val origin: GuiView?
    
    /**
     * The view we're navigating to (target, this view).
     */
    val target: GuiView
        get() = view
}
