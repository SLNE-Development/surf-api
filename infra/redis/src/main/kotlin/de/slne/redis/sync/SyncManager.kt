package de.slne.redis.sync

import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.*
import kotlinx.serialization.serializer
import kotlin.reflect.KClass

/**
 * Global manager for synchronized structures.
 * Manages the shared Redis connection and provides simplified factory methods.
 */
object SyncManager {
    private var redisUri: String = "redis://localhost:6379"
    
    /**
     * Initialize the sync manager with a Redis URI.
     * This should be called once at application startup.
     * 
     * @param uri The Redis connection URI
     */
    fun init(uri: String) {
        redisUri = uri
    }
    
    /**
     * Get the current Redis URI.
     */
    fun getRedisUri(): String = redisUri
    
    /**
     * Get a serializer for common primitive types.
     * Throws IllegalArgumentException for unsupported types.
     */
    @Suppress("UNCHECKED_CAST")
    inline fun <reified T> getSerializer(): KSerializer<T> {
        return when (T::class) {
            String::class -> String.serializer()
            Int::class -> Int.serializer()
            Long::class -> Long.serializer()
            Float::class -> Float.serializer()
            Double::class -> Double.serializer()
            Boolean::class -> Boolean.serializer()
            Byte::class -> Byte.serializer()
            Short::class -> Short.serializer()
            Char::class -> Char.serializer()
            else -> {
                // Try to get serializer for @Serializable classes
                try {
                    serializer<T>()
                } catch (e: Exception) {
                    throw IllegalArgumentException(
                        "No serializer found for type ${T::class.simpleName}. " +
                        "Either use a primitive type (String, Int, etc.) or a @Serializable class, " +
                        "or provide a custom serializer."
                    )
                }
            }
        } as KSerializer<T>
    }
}

/**
 * Create a SyncValue with automatic serializer inference and global Redis connection.
 * 
 * @param id The unique identifier for this value
 * @param defaultValue The default value
 * @return A new SyncValue instance
 */
inline fun <reified T> syncValue(id: String, defaultValue: T): SyncValue<T> {
    return SyncValue(
        id = id,
        defaultValue = defaultValue,
        redisUri = SyncManager.getRedisUri(),
        serializer = SyncManager.getSerializer<T>()
    )
}

/**
 * Create a SyncSet with automatic serializer inference and global Redis connection.
 * 
 * @param id The unique identifier for this set
 * @return A new SyncSet instance
 */
inline fun <reified T> syncSet(id: String): SyncSet<T> {
    return SyncSet(
        id = id,
        redisUri = SyncManager.getRedisUri(),
        serializer = SyncManager.getSerializer<T>()
    )
}

/**
 * Create a SyncMap with automatic serializer inference and global Redis connection.
 * 
 * @param id The unique identifier for this map
 * @return A new SyncMap instance
 */
inline fun <reified K, reified V> syncMap(id: String): SyncMap<K, V> {
    return SyncMap(
        id = id,
        redisUri = SyncManager.getRedisUri(),
        keySerializer = SyncManager.getSerializer<K>(),
        valueSerializer = SyncManager.getSerializer<V>()
    )
}

/**
 * Create a SyncList with automatic serializer inference and global Redis connection.
 * 
 * @param id The unique identifier for this list
 * @return A new SyncList instance
 */
inline fun <reified T> syncList(id: String): SyncList<T> {
    return SyncList(
        id = id,
        redisUri = SyncManager.getRedisUri(),
        serializer = SyncManager.getSerializer<T>()
    )
}
