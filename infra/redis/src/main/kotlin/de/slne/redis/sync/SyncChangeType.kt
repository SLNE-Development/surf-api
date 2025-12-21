package de.slne.redis.sync

/**
 * Represents the type of change that occurred in a synchronized structure.
 */
enum class SyncChangeType {
    /**
     * A value was set or updated
     */
    SET,
    
    /**
     * A value was added
     */
    ADD,
    
    /**
     * A value was removed
     */
    REMOVE
}
