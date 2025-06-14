package dev.slne.surf.surfapi.bukkit.api.inventory.utils

enum class Priority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR;

    fun isLessThan(priority: Priority): Boolean = this.ordinal < priority.ordinal
    fun isGreaterThan(priority: Priority): Boolean = this.ordinal > priority.ordinal
}