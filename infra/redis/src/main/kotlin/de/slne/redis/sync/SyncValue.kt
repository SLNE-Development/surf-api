package de.slne.redis.sync

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * A synchronized value that is kept in sync across all Redis servers.
 * Supports type-safe operations and change notifications.
 * 
 * @param T The type of the value
 * @param id The unique identifier for this value
 * @param defaultValue The default value to use when no value is set
 * @param redisUri The Redis connection URI
 * @param serializer The serializer for type T
 */
class SyncValue<T>(
    id: String,
    private val defaultValue: T,
    redisUri: String,
    private val serializer: KSerializer<T>,
    coroutineScope: CoroutineScope? = null
) : SyncStructure<T>(
    id,
    redisUri,
    coroutineScope ?: kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
    )
) {
    
    @Volatile
    private var currentValue: T = defaultValue
    
    override val channelPrefix: String = "surf-redis:sync:value"
    
    init {
        // Try to fetch the current value from Redis on initialization
        runBlocking {
            fetchCurrentValue()
        }
    }
    
    private suspend fun fetchCurrentValue() {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val redisValue = pubConnection.sync().get("$channelPrefix:data:$id")
                if (redisValue != null) {
                    currentValue = Json.decodeFromString(serializer, redisValue)
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching current value for $id: ${e.message}")
        }
    }
    
    /**
     * Get the current value. Returns the default value if no value has been set.
     * @return The current value
     */
    fun get(): T = currentValue
    
    /**
     * Set a new value and sync it across all servers.
     * @param value The new value to set
     */
    suspend fun set(value: T) {
        currentValue = value
        
        // Store in Redis
        val serializedValue = Json.encodeToString(serializer, value)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            pubConnection.sync().set("$channelPrefix:data:$id", serializedValue)
        }
        
        // Publish change
        val message = SyncMessage(
            operation = "SET",
            data = serializedValue
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.SET, value)
    }
    
    /**
     * Set a new value synchronously (blocking).
     * @param value The new value to set
     */
    fun setBlocking(value: T) {
        runBlocking {
            set(value)
        }
    }
    
    override suspend fun handleIncomingMessage(message: String) {
        try {
            val syncMessage = Json.decodeFromString<SyncMessage>(message)
            
            when (syncMessage.operation) {
                "SET" -> {
                    val newValue = Json.decodeFromString(serializer, syncMessage.data)
                    currentValue = newValue
                    notifyListeners(SyncChangeType.SET, newValue)
                }
            }
        } catch (e: Exception) {
            System.err.println("Error handling incoming message for SyncValue $id: ${e.message}")
            e.printStackTrace()
        }
    }
}
