package dev.slne.surf.surfapi.bukkit.api.gui.context

/**
 * Context for lifecycle events.
 */
interface LifecycleContext : ViewContext {
    /**
     * The type of lifecycle event.
     */
    val eventType: LifecycleEventType
}
