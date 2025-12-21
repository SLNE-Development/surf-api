package de.slne.redis.sync

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A synchronized map that is kept in sync across all Redis servers.
 * Based on fastutil's Object2ObjectOpenHashMap for high performance.
 * Supports type-safe operations and change notifications.
 * 
 * @param K The type of keys in the map
 * @param V The type of values in the map
 * @param id The unique identifier for this map
 * @param redisUri The Redis connection URI
 * @param keySerializer The serializer for type K
 * @param valueSerializer The serializer for type V
 */
class SyncMap<K, V>(
    id: String,
    redisUri: String,
    private val keySerializer: KSerializer<K>,
    private val valueSerializer: KSerializer<V>,
    coroutineScope: CoroutineScope? = null
) : SyncStructure<V>(
    id,
    redisUri,
    coroutineScope ?: kotlinx.coroutines.CoroutineScope(
        kotlinx.coroutines.Dispatchers.Default + kotlinx.coroutines.SupervisorJob()
    )
) {
    
    private val internalMap = Object2ObjectOpenHashMap<K, V>()
    
    override val channelPrefix: String = "surf-redis:sync:map"
    
    init {
        // Try to fetch the current map from Redis on initialization
        runBlocking {
            fetchCurrentMap()
        }
    }
    
    @Serializable
    private data class MapEntry<K, V>(val key: K, val value: V)
    
    private suspend fun fetchCurrentMap() {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val redisValue = pubConnection.sync().get("$channelPrefix:data:$id")
                if (redisValue != null) {
                    val entrySerializer = kotlinx.serialization.serializer<MapEntry<K, V>>()
                    val listSerializer = ListSerializer(entrySerializer)
                    val entries = Json.decodeFromString(listSerializer, redisValue)
                    synchronized(internalMap) {
                        internalMap.clear()
                        entries.forEach { entry ->
                            internalMap[entry.key] = entry.value
                        }
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching current map for $id: ${e.message}")
        }
    }
    
    /**
     * Put a key-value pair into the map and sync it across all servers.
     * @param key The key
     * @param value The value
     * @return The previous value associated with the key, or null if there was no mapping
     */
    suspend fun put(key: K, value: V): V? {
        val oldValue = synchronized(internalMap) {
            internalMap.put(key, value)
        }
        
        // Update Redis
        persistMap()
        
        // Publish change
        val serializedKey = Json.encodeToString(keySerializer, key)
        val serializedValue = Json.encodeToString(valueSerializer, value)
        val message = SyncMessage(
            operation = "PUT",
            data = serializedValue,
            key = serializedKey
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.SET, value, key)
        
        return oldValue
    }
    
    /**
     * Put a key-value pair synchronously (blocking).
     * @param key The key
     * @param value The value
     * @return The previous value associated with the key, or null if there was no mapping
     */
    fun putBlocking(key: K, value: V): V? {
        return runBlocking {
            put(key, value)
        }
    }
    
    /**
     * Get the value associated with a key.
     * @param key The key
     * @return The value, or null if the key is not present
     */
    fun get(key: K): V? {
        return synchronized(internalMap) {
            internalMap[key]
        }
    }
    
    /**
     * Remove a key-value pair from the map and sync it across all servers.
     * @param key The key to remove
     * @return The value that was associated with the key, or null if there was no mapping
     */
    suspend fun remove(key: K): V? {
        val removedValue = synchronized(internalMap) {
            internalMap.remove(key)
        }
        
        if (removedValue != null) {
            // Update Redis
            persistMap()
            
            // Publish change
            val serializedKey = Json.encodeToString(keySerializer, key)
            val message = SyncMessage(
                operation = "REMOVE",
                data = "",
                key = serializedKey
            )
            publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
            
            // Notify local listeners
            notifyListeners(SyncChangeType.REMOVE, removedValue, key)
        }
        
        return removedValue
    }
    
    /**
     * Remove a key-value pair synchronously (blocking).
     * @param key The key to remove
     * @return The value that was associated with the key, or null if there was no mapping
     */
    fun removeBlocking(key: K): V? {
        return runBlocking {
            remove(key)
        }
    }
    
    /**
     * Check if the map contains a key.
     * @param key The key to check
     * @return true if the key is present
     */
    fun containsKey(key: K): Boolean {
        return synchronized(internalMap) {
            internalMap.containsKey(key)
        }
    }
    
    /**
     * Check if the map contains a value.
     * @param value The value to check
     * @return true if the value is present
     */
    fun containsValue(value: V): Boolean {
        return synchronized(internalMap) {
            internalMap.containsValue(value)
        }
    }
    
    /**
     * Get the size of the map.
     * @return The number of key-value pairs in the map
     */
    fun size(): Int {
        return synchronized(internalMap) {
            internalMap.size
        }
    }
    
    /**
     * Check if the map is empty.
     * @return true if the map contains no key-value pairs
     */
    fun isEmpty(): Boolean {
        return synchronized(internalMap) {
            internalMap.isEmpty()
        }
    }
    
    /**
     * Clear all key-value pairs from the map and sync across all servers.
     */
    suspend fun clear() {
        synchronized(internalMap) {
            internalMap.clear()
        }
        
        // Update Redis
        persistMap()
        
        // Publish change
        val message = SyncMessage(
            operation = "CLEAR",
            data = ""
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
    }
    
    /**
     * Get all keys in the map.
     * @return A new set containing all keys
     */
    fun keys(): Set<K> {
        return synchronized(internalMap) {
            HashSet(internalMap.keys)
        }
    }
    
    /**
     * Get all values in the map.
     * @return A new collection containing all values
     */
    fun values(): Collection<V> {
        return synchronized(internalMap) {
            ArrayList(internalMap.values)
        }
    }
    
    /**
     * Get a snapshot of all entries in the map.
     * @return A new map containing all entries
     */
    fun toMap(): Map<K, V> {
        return synchronized(internalMap) {
            HashMap(internalMap)
        }
    }
    
    private suspend fun persistMap() {
        try {
            val entries = synchronized(internalMap) {
                internalMap.map { (k, v) -> MapEntry(k, v) }
            }
            val entrySerializer = kotlinx.serialization.serializer<MapEntry<K, V>>()
            val listSerializer = ListSerializer(entrySerializer)
            val serializedList = Json.encodeToString(listSerializer, entries)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                pubConnection.sync().set("$channelPrefix:data:$id", serializedList)
            }
        } catch (e: Exception) {
            System.err.println("Error persisting map for $id: ${e.message}")
        }
    }
    
    override suspend fun handleIncomingMessage(message: String) {
        try {
            val syncMessage = Json.decodeFromString<SyncMessage>(message)
            
            when (syncMessage.operation) {
                "PUT" -> {
                    val key = Json.decodeFromString(keySerializer, syncMessage.key!!)
                    val value = Json.decodeFromString(valueSerializer, syncMessage.data)
                    synchronized(internalMap) {
                        internalMap[key] = value
                    }
                    notifyListeners(SyncChangeType.SET, value, key)
                }
                "REMOVE" -> {
                    val key = Json.decodeFromString(keySerializer, syncMessage.key!!)
                    val removedValue = synchronized(internalMap) {
                        internalMap.remove(key)
                    }
                    if (removedValue != null) {
                        notifyListeners(SyncChangeType.REMOVE, removedValue, key)
                    }
                }
                "CLEAR" -> {
                    synchronized(internalMap) {
                        internalMap.clear()
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error handling incoming message for SyncMap $id: ${e.message}")
            e.printStackTrace()
        }
    }
}
