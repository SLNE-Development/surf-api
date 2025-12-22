package de.slne.redis.example.sync

import de.slne.redis.sync.*
import de.slne.redis.sync.list.SyncList
import de.slne.redis.sync.map.SyncMap
import de.slne.redis.sync.set.SyncSet
import de.slne.redis.sync.value.SyncValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

/**
 * Example usage of synchronized data structures.
 * This demonstrates how to use SyncValue, SyncSet, SyncMap, and SyncList
 * to synchronize data across multiple servers.
 */
fun main() = runBlocking {
    val redisUri = "redis://localhost:6379"
    
    println("=== SyncValue Example ===")
    syncValueExample(redisUri)
    
    println("\n=== SyncSet Example ===")
    syncSetExample(redisUri)
    
    println("\n=== SyncMap Example ===")
    syncMapExample(redisUri)
    
    println("\n=== SyncList Example ===")
    syncListExample(redisUri)
}

/**
 * Example using SyncValue with a default value.
 * SyncValue synchronizes a single value across all servers.
 */
suspend fun syncValueExample(redisUri: String) {
    // Create a synchronized value with a default value
    val serverStatus = SyncValue(
        id = "server-status",
        defaultValue = "offline",
        redisUri = redisUri,
        serializer = String.serializer()
    )
    
    // Subscribe to changes
    serverStatus.subscribe { changeType, value, _ ->
        println("Server status changed: $changeType -> $value")
    }
    
    // Get the current value (returns default if not set)
    println("Current status: ${serverStatus.get()}")
    
    // Set a new value (syncs across all servers)
    serverStatus.set("online")
    println("Updated status: ${serverStatus.get()}")
    
    delay(100) // Allow time for async operations
    serverStatus.close()
}

/**
 * Example using SyncSet with type safety.
 * SyncSet synchronizes a set of unique values across all servers.
 */
suspend fun syncSetExample(redisUri: String) {
    // Create a synchronized set for player names
    val onlinePlayers = SyncSet(
        id = "online-players",
        redisUri = redisUri,
        serializer = String.serializer()
    )
    
    // Subscribe to player join/leave events
    onlinePlayers.subscribe { changeType, player, _ ->
        when (changeType) {
            SyncChangeType.ADD -> println("$player joined the server")
            SyncChangeType.REMOVE -> println("$player left the server")
            else -> {}
        }
    }
    
    // Add players (syncs across all servers)
    onlinePlayers.add("Steve")
    onlinePlayers.add("Alex")
    onlinePlayers.add("Notch")
    
    println("Online players: ${onlinePlayers.toSet()}")
    println("Player count: ${onlinePlayers.size()}")
    
    // Check if a player is online
    println("Is Steve online? ${onlinePlayers.contains("Steve")}")
    
    // Remove a player (syncs across all servers)
    onlinePlayers.remove("Steve")
    println("After Steve left: ${onlinePlayers.toSet()}")
    
    delay(100) // Allow time for async operations
    onlinePlayers.close()
}

/**
 * Example using SyncMap with custom serializable types.
 * SyncMap synchronizes key-value pairs across all servers.
 */
suspend fun syncMapExample(redisUri: String) {
    @Serializable
    data class PlayerData(val level: Int, val coins: Int)
    
    // Create a synchronized map for player data
    val playerData = SyncMap(
        id = "player-data",
        redisUri = redisUri,
        keySerializer = String.serializer(),
        valueSerializer = PlayerData.serializer()
    )
    
    // Subscribe to player data changes
    playerData.subscribe { changeType, data, key ->
        when (changeType) {
            SyncChangeType.SET -> println("Player $key data updated: $data")
            SyncChangeType.REMOVE -> println("Player $key data removed")
            else -> {}
        }
    }
    
    // Add player data (syncs across all servers)
    playerData.put("Steve", PlayerData(level = 10, coins = 100))
    playerData.put("Alex", PlayerData(level = 15, coins = 200))
    
    println("Player data: ${playerData.toMap()}")
    
    // Get specific player data
    val steveData = playerData.get("Steve")
    println("Steve's data: $steveData")
    
    // Update player data (syncs across all servers)
    playerData.put("Steve", PlayerData(level = 11, coins = 150))
    
    // Check if a player exists
    println("Does Alex exist? ${playerData.containsKey("Alex")}")
    
    // Remove player data (syncs across all servers)
    playerData.remove("Steve")
    println("After removing Steve: ${playerData.toMap()}")
    
    delay(100) // Allow time for async operations
    playerData.close()
}

/**
 * Example using SyncList with ordering.
 * SyncList synchronizes an ordered list of values across all servers.
 */
suspend fun syncListExample(redisUri: String) {
    // Create a synchronized list for server messages
    val messageLog = SyncList(
        id = "message-log",
        redisUri = redisUri,
        serializer = String.serializer()
    )
    
    // Subscribe to new messages
    messageLog.subscribe { changeType, message, index ->
        when (changeType) {
            SyncChangeType.ADD -> println("New message added at index $index: $message")
            SyncChangeType.REMOVE -> println("Message removed: $message")
            SyncChangeType.SET -> println("Message updated at index $index: $message")
        }
    }
    
    // Add messages (syncs across all servers)
    messageLog.add("Server started")
    messageLog.add("Player Steve joined")
    messageLog.add("Player Alex joined")
    
    println("Message log: ${messageLog.toList()}")
    println("Message count: ${messageLog.size()}")
    
    // Get a specific message
    println("First message: ${messageLog.get(0)}")
    
    // Update a message at a specific index (syncs across all servers)
    messageLog.set(1, "Player Steve joined (VIP)")
    
    // Add a message at a specific index (syncs across all servers)
    messageLog.add(1, "Server configuration loaded")
    
    println("Updated log: ${messageLog.toList()}")
    
    // Remove a message (syncs across all servers)
    messageLog.removeAt(0)
    println("After removal: ${messageLog.toList()}")
    
    delay(100) // Allow time for async operations
    messageLog.close()
}
