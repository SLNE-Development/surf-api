package dev.slne.surf.api.core.event

/**
 * Common parent type for every event dispatched through [SurfEventBus].
 *
 * Plugins are expected to implement either [SurfSyncEvent] or [SurfAsyncEvent]
 * (or extend the corresponding abstract base classes), not this interface
 * directly.
 *
 * The bus is intentionally platform-independent and lives in `surf-api-core`,
 * so it must not depend on Bukkit/Velocity event APIs.
 */
interface SurfEvent
