package dev.slne.surf.api.core.event

/**
 * Priority for [SurfEventHandler]s. Handlers are invoked in priority order
 * from [LOWEST] to [MONITOR].
 *
 * [MONITOR] handlers are intended for read-only observation only – they should
 * not mutate the event. The bus does not enforce this contract, it is a
 * convention adopted from comparable Bukkit / Sponge APIs.
 *
 * The bus is intentionally platform independent, so this enum does not depend
 * on Bukkit's `EventPriority`.
 */
enum class SurfEventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR,
}
