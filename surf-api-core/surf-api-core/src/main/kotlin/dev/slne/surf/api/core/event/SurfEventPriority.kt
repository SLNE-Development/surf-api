package dev.slne.surf.api.core.event

/**
 * Defines the execution priority of a [SurfEventHandler].
 *
 * Handlers are invoked in ascending order, starting at [LOWEST] and ending at
 * [MONITOR].
 *
 * Typical usage:
 * - [LOWEST]/[LOW]: early preprocessing or default value initialization.
 * - [NORMAL]: default priority for most handlers.
 * - [HIGH]/[HIGHEST]: late-stage overrides or validation after other handlers.
 * - [MONITOR]: final observation/logging only; always invoked even when
 *   `ignoreCancelled` is `true`.
 *
 * [MONITOR] handlers are intended to be read-only and should not mutate event
 * state. This is a behavioral convention and is not enforced by the event bus.
 */
enum class SurfEventPriority {
    LOWEST,
    LOW,
    NORMAL,
    HIGH,
    HIGHEST,
    MONITOR,
}