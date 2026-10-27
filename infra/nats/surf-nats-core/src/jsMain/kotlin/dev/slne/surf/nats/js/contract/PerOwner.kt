package dev.slne.surf.nats.js.contract

import dev.slne.surf.nats.protocol.ProtocolNaming

/**
 * Resolves and caches contract values for individual owning clients.
 *
 * Global contracts share a single value, resolved during initialization.
 * Non-global contracts require a valid owner and cache the resolved value per client.
 * Missing or invalid owners are rejected, and failed resolutions are not cached.
 *
 * Not thread-safe; intended for single-threaded Kotlin/JS execution.
 */
internal class PerOwner<T : Any>(
    global: Boolean,
    private val typeName: String,
    private val resolve: (owner: String?) -> T,
) {
    private val globalValue = if (global) resolve(null) else null
    private val values = mutableMapOf<String, T>()

    operator fun get(owner: String?): T {
        globalValue?.let { return it }

        requireNotNull(owner) {
            "$typeName is not global: pass the owning client name as 'owner' or use target(owner)"
        }

        return values.getOrPut(owner) {
            resolve(ProtocolNaming.requireClientName(owner, "owner"))
        }
    }
}
