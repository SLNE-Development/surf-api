package dev.slne.surf.surfapi.bukkit.api.inventory.utils

enum class Priority {

    LOWEST {
        override fun isLessThan(priority: Priority) = this != priority
    },

    LOW {
        override fun isLessThan(priority: Priority) = this != priority && priority != LOWEST
    },

    NORMAL {
        override fun isLessThan(priority: Priority) =
            this != priority && priority != LOWEST && priority != LOW
    },

    HIGH {
        override fun isLessThan(priority: Priority) =
            this != priority && priority != LOWEST && priority != LOW && priority != NORMAL
    },

    HIGHEST {
        override fun isLessThan(priority: Priority) =
            this != priority && priority != LOWEST && priority != LOW && priority != NORMAL && priority != HIGH
    },

    MONITOR {
        override fun isLessThan(priority: Priority) = false
    };

    abstract fun isLessThan(priority: Priority): Boolean
    fun isGreaterThan(priority: Priority) = !isLessThan(priority) && this != priority


}