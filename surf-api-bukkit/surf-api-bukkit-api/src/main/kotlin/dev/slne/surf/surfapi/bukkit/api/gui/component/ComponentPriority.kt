package dev.slne.surf.surfapi.bukkit.api.gui.component

/**
 * Priority levels for components.
 * When multiple components overlap at the same slot, the one with highest priority
 * is rendered and handles click events.
 */
enum class ComponentPriority(val value: Int) {
    /**
     * Lowest priority - background elements.
     */
    LOWEST(0),
    
    /**
     * Low priority.
     */
    LOW(25),
    
    /**
     * Normal priority - default for most components.
     */
    NORMAL(50),
    
    /**
     * High priority.
     */
    HIGH(75),
    
    /**
     * Highest priority - overlay elements.
     */
    HIGHEST(100);
    
    companion object {
        /**
         * Get priority by value, defaults to NORMAL.
         */
        fun fromValue(value: Int): ComponentPriority {
            return entries.firstOrNull { it.value == value } ?: NORMAL
        }
    }
}
