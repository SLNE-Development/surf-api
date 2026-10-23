package dev.slne.surf.nats.js

import dev.slne.surf.nats.protocol.ProtocolNaming

/**
 * Memoizes a value of the contract [typeName] per owning client; a global contract has one value for every owner.
 *
 * A missing or invalid owner of a non-global contract fails on every use; only valid owners are cached. Not
 * thread-safe (the JS runtime is single-threaded).
 */
internal class PerOwner<T : Any>(
    global: Boolean,
    private val typeName: String,
    private val resolve: (owner: String?) -> T,
) {
    private val global: T? = if (global) resolve(null) else null
    private val values = mutableMapOf<String, T>()

    operator fun get(owner: String?): T {
        global?.let { return it }
        requireNotNull(owner) {
            "$typeName is not global: pass the owning client name as 'owner' or use target(owner)"
        }
        return values.getOrPut(owner) { resolve(ProtocolNaming.requireClientName(owner, "owner")) }
    }
}
