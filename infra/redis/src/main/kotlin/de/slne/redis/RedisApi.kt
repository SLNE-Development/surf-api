package de.slne.redis

import io.lettuce.core.RedisClient
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection

/**
 * Global Redis API for managing Redis connections.
 * Provides a centralized way to initialize and access Redis connections.
 * 
 * Usage:
 * ```
 * RedisApi.init(url = "redis://localhost:6379")
 * // or
 * RedisApi(url = "redis://localhost:6379").connect()
 * ```
 */
object RedisApi {
    private var redisClient: RedisClient? = null
    private var redisUrl: String = "redis://localhost:6379"
    private var isInitialized = false
    
    /**
     * Initialize the Redis API with a connection URL.
     * This should be called once at application startup.
     * 
     * @param url The Redis connection URL (e.g., "redis://localhost:6379")
     * @return This RedisApi instance for chaining
     */
    fun init(url: String = "redis://localhost:6379"): RedisApi {
        if (isInitialized && redisUrl == url) {
            return this
        }
        
        // Close existing connection if any
        if (isInitialized) {
            disconnect()
        }
        
        redisUrl = url
        connect()
        return this
    }
    
    /**
     * Alternative syntax for initialization.
     * 
     * @param url The Redis connection URL
     * @return This RedisApi instance
     */
    operator fun invoke(url: String): RedisApi {
        return init(url)
    }
    
    /**
     * Connect to Redis using the configured URL.
     * Called automatically by init().
     * 
     * @return This RedisApi instance for chaining
     */
    fun connect(): RedisApi {
        if (!isInitialized) {
            redisClient = RedisClient.create(redisUrl)
            isInitialized = true
        }
        return this
    }
    
    /**
     * Disconnect from Redis and clean up resources.
     */
    fun disconnect() {
        redisClient?.shutdown()
        redisClient = null
        isInitialized = false
    }
    
    /**
     * Get the Redis client instance.
     * Automatically initializes with default URL if not already initialized.
     * 
     * @return The RedisClient instance
     */
    fun getClient(): RedisClient {
        if (!isInitialized) {
            init()
        }
        return redisClient ?: throw IllegalStateException("Redis client not initialized")
    }
    
    /**
     * Get the configured Redis URL.
     * 
     * @return The Redis connection URL
     */
    fun getUrl(): String = redisUrl
    
    /**
     * Check if Redis is initialized and connected.
     * 
     * @return True if initialized, false otherwise
     */
    fun isConnected(): Boolean = isInitialized && redisClient != null
    
    /**
     * Create a new stateful connection for Redis commands.
     * 
     * @return A new StatefulRedisConnection
     */
    fun createConnection(): StatefulRedisConnection<String, String> {
        return getClient().connect()
    }
    
    /**
     * Create a new stateful pub/sub connection for Redis pub/sub.
     * 
     * @return A new StatefulRedisPubSubConnection
     */
    fun createPubSubConnection(): StatefulRedisPubSubConnection<String, String> {
        return getClient().connectPubSub()
    }
}
