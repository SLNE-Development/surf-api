package de.slne.redis.example.sync

import de.slne.redis.sync.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable

/**
 * Example usage of synchronized data structures with simplified API.
 * This demonstrates the easy-to-use factory functions with automatic serializer inference.
 */
fun main() = runBlocking {
    // Initialize the sync manager once at startup
    SyncManager.init("redis://localhost:6379")
    
    println("=== Simplified SyncValue Example ===")
    simplifiedSyncValueExample()
    
    println("\n=== Simplified SyncSet Example ===")
    simplifiedSyncSetExample()
    
    println("\n=== Simplified SyncMap Example ===")
    simplifiedSyncMapExample()
    
    println("\n=== Simplified SyncList Example ===")
    simplifiedSyncListExample()
}

/**
 * Example using simplified SyncValue API.
 */
suspend fun simplifiedSyncValueExample() {
    // Create a synchronized value with automatic serializer inference
    val serverStatus = syncValue("server-status", "offline")
    
    // Subscribe to changes
    serverStatus.subscribe { changeType, value, _ ->
        println("Status changed: $changeType -> $value")
    }
    
    // Get the current value
    println("Current status: ${serverStatus.get()}")
    
    // Set a new value (syncs across all servers)
    serverStatus.set("online")
    println("Updated status: ${serverStatus.get()}")
    
    delay(100)
    serverStatus.close()
}

/**
 * Example using simplified SyncSet API.
 */
suspend fun simplifiedSyncSetExample() {
    // Create a synchronized set with automatic serializer inference
    val onlinePlayers = syncSet<String>("online-players")
    
    // Subscribe to changes
    onlinePlayers.subscribe { changeType, player, _ ->
        when (changeType) {
            SyncChangeType.ADD -> println("$player joined")
            SyncChangeType.REMOVE -> println("$player left")
            else -> {}
        }
    }
    
    // Add players
    onlinePlayers.add("Steve")
    onlinePlayers.add("Alex")
    
    println("Online players: ${onlinePlayers.toSet()}")
    println("Player count: ${onlinePlayers.size()}")
    
    delay(100)
    onlinePlayers.close()
}

/**
 * Example using simplified SyncMap API with custom types.
 */
suspend fun simplifiedSyncMapExample() {
    @Serializable
    data class PlayerData(val level: Int, val coins: Int)
    
    // Create a synchronized map with automatic serializer inference
    val playerData = syncMap<String, PlayerData>("player-data")
    
    // Subscribe to changes
    playerData.subscribe { changeType, data, key ->
        println("Player $key: $changeType -> $data")
    }
    
    // Add player data
    playerData.put("Steve", PlayerData(level = 10, coins = 100))
    playerData.put("Alex", PlayerData(level = 15, coins = 200))
    
    println("Player data: ${playerData.toMap()}")
    
    // Get specific player data
    val steveData = playerData.get("Steve")
    println("Steve's data: $steveData")
    
    delay(100)
    playerData.close()
}

/**
 * Example using simplified SyncList API.
 */
suspend fun simplifiedSyncListExample() {
    // Create a synchronized list with automatic serializer inference
    val messageLog = syncList<String>("message-log")
    
    // Subscribe to changes
    messageLog.subscribe { changeType, message, index ->
        println("Message $changeType at index $index: $message")
    }
    
    // Add messages
    messageLog.add("Server started")
    messageLog.add("Player Steve joined")
    messageLog.add("Player Alex joined")
    
    println("Message log: ${messageLog.toList()}")
    println("Message count: ${messageLog.size()}")
    
    // Get a specific message
    println("First message: ${messageLog.get(0)}")
    
    delay(100)
    messageLog.close()
}
