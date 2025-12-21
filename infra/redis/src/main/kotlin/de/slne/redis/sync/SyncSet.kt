package de.slne.redis.sync

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A synchronized set that is kept in sync across all Redis servers.
 * Based on fastutil's ObjectOpenHashSet for high performance.
 * Supports type-safe operations and change notifications.
 * 
 * @param T The type of elements in the set
 * @param id The unique identifier for this set
 * @param redisUri The Redis connection URI
 * @param serializer The serializer for type T
 */
class SyncSet<T>(
    id: String,
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
    
    private val internalSet = ObjectOpenHashSet<T>()
    
    override val channelPrefix: String = "surf-redis:sync:set"
    
    init {
        // Try to fetch the current set from Redis on initialization
        runBlocking {
            fetchCurrentSet()
        }
    }
    
    private suspend fun fetchCurrentSet() {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val redisValue = pubConnection.sync().get("$channelPrefix:data:$id")
                if (redisValue != null) {
                    val listSerializer = ListSerializer(serializer)
                    val list = Json.decodeFromString(listSerializer, redisValue)
                    synchronized(internalSet) {
                        internalSet.clear()
                        internalSet.addAll(list)
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching current set for $id: ${e.message}")
        }
    }
    
    /**
     * Add an element to the set and sync it across all servers.
     * @param element The element to add
     * @return true if the element was added, false if it was already present
     */
    suspend fun add(element: T): Boolean {
        val added = synchronized(internalSet) {
            internalSet.add(element)
        }
        
        if (added) {
            // Update Redis
            persistSet()
            
            // Publish change
            val serializedElement = Json.encodeToString(serializer, element)
            val message = SyncMessage(
                operation = "ADD",
                data = serializedElement
            )
            publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
            
            // Notify local listeners
            notifyListeners(SyncChangeType.ADD, element)
        }
        
        return added
    }
    
    /**
     * Add an element synchronously (blocking).
     * @param element The element to add
     * @return true if the element was added, false if it was already present
     */
    fun addBlocking(element: T): Boolean {
        return runBlocking {
            add(element)
        }
    }
    
    /**
     * Remove an element from the set and sync it across all servers.
     * @param element The element to remove
     * @return true if the element was removed, false if it was not present
     */
    suspend fun remove(element: T): Boolean {
        val removed = synchronized(internalSet) {
            internalSet.remove(element)
        }
        
        if (removed) {
            // Update Redis
            persistSet()
            
            // Publish change
            val serializedElement = Json.encodeToString(serializer, element)
            val message = SyncMessage(
                operation = "REMOVE",
                data = serializedElement
            )
            publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
            
            // Notify local listeners
            notifyListeners(SyncChangeType.REMOVE, element)
        }
        
        return removed
    }
    
    /**
     * Remove an element synchronously (blocking).
     * @param element The element to remove
     * @return true if the element was removed, false if it was not present
     */
    fun removeBlocking(element: T): Boolean {
        return runBlocking {
            remove(element)
        }
    }
    
    /**
     * Check if the set contains an element.
     * @param element The element to check
     * @return true if the element is present
     */
    fun contains(element: T): Boolean {
        return synchronized(internalSet) {
            internalSet.contains(element)
        }
    }
    
    /**
     * Get the size of the set.
     * @return The number of elements in the set
     */
    fun size(): Int {
        return synchronized(internalSet) {
            internalSet.size
        }
    }
    
    /**
     * Check if the set is empty.
     * @return true if the set contains no elements
     */
    fun isEmpty(): Boolean {
        return synchronized(internalSet) {
            internalSet.isEmpty()
        }
    }
    
    /**
     * Clear all elements from the set and sync across all servers.
     */
    suspend fun clear() {
        synchronized(internalSet) {
            internalSet.clear()
        }
        
        // Update Redis
        persistSet()
        
        // Publish change
        val message = SyncMessage(
            operation = "CLEAR",
            data = ""
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
    }
    
    /**
     * Get a snapshot of all elements in the set.
     * @return A new set containing all elements
     */
    fun toSet(): Set<T> {
        return synchronized(internalSet) {
            HashSet(internalSet)
        }
    }
    
    private suspend fun persistSet() {
        try {
            val list = synchronized(internalSet) {
                internalSet.toList()
            }
            val listSerializer = ListSerializer(serializer)
            val serializedList = Json.encodeToString(listSerializer, list)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                pubConnection.sync().set("$channelPrefix:data:$id", serializedList)
            }
        } catch (e: Exception) {
            System.err.println("Error persisting set for $id: ${e.message}")
        }
    }
    
    override suspend fun handleIncomingMessage(message: String) {
        try {
            val syncMessage = Json.decodeFromString<SyncMessage>(message)
            
            when (syncMessage.operation) {
                "ADD" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    synchronized(internalSet) {
                        internalSet.add(element)
                    }
                    notifyListeners(SyncChangeType.ADD, element)
                }
                "REMOVE" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    synchronized(internalSet) {
                        internalSet.remove(element)
                    }
                    notifyListeners(SyncChangeType.REMOVE, element)
                }
                "CLEAR" -> {
                    synchronized(internalSet) {
                        internalSet.clear()
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error handling incoming message for SyncSet $id: ${e.message}")
            e.printStackTrace()
        }
    }
}
