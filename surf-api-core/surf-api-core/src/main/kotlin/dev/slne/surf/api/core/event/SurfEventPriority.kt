package dev.slne.surf.api.core.event

/** Priority ordering for event handler execution. Lower ordinals execute first. */
enum class SurfEventPriority { LOWEST, LOW, NORMAL, HIGH, HIGHEST, MONITOR }
