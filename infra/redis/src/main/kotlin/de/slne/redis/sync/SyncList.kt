package de.slne.redis.sync

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * A synchronized list that is kept in sync across all Redis servers.
 * Based on fastutil's ObjectArrayList for high performance.
 * Supports type-safe operations and change notifications.
 * 
 * @param T The type of elements in the list
 * @param id The unique identifier for this list
 * @param redisUri The Redis connection URI
 * @param serializer The serializer for type T
 */
class SyncList<T>(
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
    
    private val internalList = ObjectArrayList<T>()
    
    override val channelPrefix: String = "surf-redis:sync:list"
    
    init {
        // Try to fetch the current list from Redis on initialization
        runBlocking {
            fetchCurrentList()
        }
    }
    
    private suspend fun fetchCurrentList() {
        try {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                val redisValue = pubConnection.sync().get("$channelPrefix:data:$id")
                if (redisValue != null) {
                    val listSerializer = ListSerializer(serializer)
                    val list = Json.decodeFromString(listSerializer, redisValue)
                    synchronized(internalList) {
                        internalList.clear()
                        internalList.addAll(list)
                    }
                }
            }
        } catch (e: Exception) {
            System.err.println("Error fetching current list for $id: ${e.message}")
        }
    }
    
    /**
     * Add an element to the end of the list and sync it across all servers.
     * @param element The element to add
     * @return true (as per the general contract of Collection.add)
     */
    suspend fun add(element: T): Boolean {
        val index = synchronized(internalList) {
            internalList.add(element)
            internalList.size - 1
        }
        
        // Update Redis
        persistList()
        
        // Publish change
        val serializedElement = Json.encodeToString(serializer, element)
        val message = SyncMessage(
            operation = "ADD",
            data = serializedElement,
            key = index.toString()
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.ADD, element)
        
        return true
    }
    
    /**
     * Add an element synchronously (blocking).
     * @param element The element to add
     * @return true (as per the general contract of Collection.add)
     */
    fun addBlocking(element: T): Boolean {
        return runBlocking {
            add(element)
        }
    }
    
    /**
     * Add an element at a specific index and sync it across all servers.
     * @param index The index at which to insert the element
     * @param element The element to add
     */
    suspend fun add(index: Int, element: T) {
        synchronized(internalList) {
            internalList.add(index, element)
        }
        
        // Update Redis
        persistList()
        
        // Publish change
        val serializedElement = Json.encodeToString(serializer, element)
        val message = SyncMessage(
            operation = "ADD_AT",
            data = serializedElement,
            key = index.toString()
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.ADD, element, index)
    }
    
    /**
     * Add an element at a specific index synchronously (blocking).
     * @param index The index at which to insert the element
     * @param element The element to add
     */
    fun addBlocking(index: Int, element: T) {
        runBlocking {
            add(index, element)
        }
    }
    
    /**
     * Set the element at a specific index and sync it across all servers.
     * @param index The index of the element to replace
     * @param element The new element
     * @return The element previously at the specified position
     */
    suspend fun set(index: Int, element: T): T {
        val oldValue = synchronized(internalList) {
            internalList.set(index, element)
        }
        
        // Update Redis
        persistList()
        
        // Publish change
        val serializedElement = Json.encodeToString(serializer, element)
        val message = SyncMessage(
            operation = "SET",
            data = serializedElement,
            key = index.toString()
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.SET, element, index)
        
        return oldValue
    }
    
    /**
     * Set the element at a specific index synchronously (blocking).
     * @param index The index of the element to replace
     * @param element The new element
     * @return The element previously at the specified position
     */
    fun setBlocking(index: Int, element: T): T {
        return runBlocking {
            set(index, element)
        }
    }
    
    /**
     * Get the element at a specific index.
     * @param index The index of the element to return
     * @return The element at the specified position
     */
    fun get(index: Int): T {
        return synchronized(internalList) {
            internalList[index]
        }
    }
    
    /**
     * Remove the element at a specific index and sync it across all servers.
     * @param index The index of the element to remove
     * @return The element that was removed
     */
    suspend fun removeAt(index: Int): T {
        val removedElement = synchronized(internalList) {
            internalList.removeAt(index)
        }
        
        // Update Redis
        persistList()
        
        // Publish change
        val message = SyncMessage(
            operation = "REMOVE_AT",
            data = "",
            key = index.toString()
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
        
        // Notify local listeners
        notifyListeners(SyncChangeType.REMOVE, removedElement, index)
        
        return removedElement
    }
    
    /**
     * Remove the element at a specific index synchronously (blocking).
     * @param index The index of the element to remove
     * @return The element that was removed
     */
    fun removeAtBlocking(index: Int): T {
        return runBlocking {
            removeAt(index)
        }
    }
    
    /**
     * Remove the first occurrence of an element and sync it across all servers.
     * @param element The element to remove
     * @return true if the element was removed, false if it was not present
     */
    suspend fun remove(element: T): Boolean {
        val removed = synchronized(internalList) {
            internalList.remove(element)
        }
        
        if (removed) {
            // Update Redis
            persistList()
            
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
     * Remove the first occurrence of an element synchronously (blocking).
     * @param element The element to remove
     * @return true if the element was removed, false if it was not present
     */
    fun removeBlocking(element: T): Boolean {
        return runBlocking {
            remove(element)
        }
    }
    
    /**
     * Check if the list contains an element.
     * @param element The element to check
     * @return true if the element is present
     */
    fun contains(element: T): Boolean {
        return synchronized(internalList) {
            internalList.contains(element)
        }
    }
    
    /**
     * Get the size of the list.
     * @return The number of elements in the list
     */
    fun size(): Int {
        return synchronized(internalList) {
            internalList.size
        }
    }
    
    /**
     * Check if the list is empty.
     * @return true if the list contains no elements
     */
    fun isEmpty(): Boolean {
        return synchronized(internalList) {
            internalList.isEmpty()
        }
    }
    
    /**
     * Clear all elements from the list and sync across all servers.
     */
    suspend fun clear() {
        synchronized(internalList) {
            internalList.clear()
        }
        
        // Update Redis
        persistList()
        
        // Publish change
        val message = SyncMessage(
            operation = "CLEAR",
            data = ""
        )
        publishMessage(Json.encodeToString(SyncMessage.serializer(), message))
    }
    
    /**
     * Get the index of the first occurrence of an element.
     * @param element The element to search for
     * @return The index of the first occurrence, or -1 if not found
     */
    fun indexOf(element: T): Int {
        return synchronized(internalList) {
            internalList.indexOf(element)
        }
    }
    
    /**
     * Get a snapshot of all elements in the list.
     * @return A new list containing all elements
     */
    fun toList(): List<T> {
        return synchronized(internalList) {
            ArrayList(internalList)
        }
    }
    
    private suspend fun persistList() {
        try {
            val list = synchronized(internalList) {
                internalList.toList()
            }
            val listSerializer = ListSerializer(serializer)
            val serializedList = Json.encodeToString(listSerializer, list)
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                pubConnection.sync().set("$channelPrefix:data:$id", serializedList)
            }
        } catch (e: Exception) {
            System.err.println("Error persisting list for $id: ${e.message}")
        }
    }
    
    override suspend fun handleIncomingMessage(message: String) {
        try {
            val syncMessage = Json.decodeFromString<SyncMessage>(message)
            
            when (syncMessage.operation) {
                "ADD" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    synchronized(internalList) {
                        internalList.add(element)
                    }
                    notifyListeners(SyncChangeType.ADD, element)
                }
                "ADD_AT" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    val index = syncMessage.key?.toInt() ?: return
                    synchronized(internalList) {
                        internalList.add(index, element)
                    }
                    notifyListeners(SyncChangeType.ADD, element, index)
                }
                "SET" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    val index = syncMessage.key?.toInt() ?: return
                    synchronized(internalList) {
                        internalList.set(index, element)
                    }
                    notifyListeners(SyncChangeType.SET, element, index)
                }
                "REMOVE" -> {
                    val element = Json.decodeFromString(serializer, syncMessage.data)
                    synchronized(internalList) {
                        internalList.remove(element)
                    }
                    notifyListeners(SyncChangeType.REMOVE, element)
                }
                "REMOVE_AT" -> {
                    val index = syncMessage.key?.toInt() ?: return
                    val removedElement = synchronized(internalList) {
                        internalList.removeAt(index)
                    }
                    notifyListeners(SyncChangeType.REMOVE, removedElement, index)
                }
                "CLEAR" -> {
                    synchronized(internalList) {
                        internalList.clear()
                    }
                    // Note: We don't notify listeners for CLEAR as there's no single element to report
                }
            }
        } catch (e: Exception) {
            System.err.println("Error handling incoming message for SyncList $id: ${e.message}")
            e.printStackTrace()
        }
    }
}
