package de.slne.redis.example

import de.slne.redis.RedisApi
import de.slne.redis.event.RedisEventBus
import de.slne.redis.event.OnRedisEvent
import io.lettuce.core.RedisURI
import kotlinx.coroutines.runBlocking

/**
 * Example listener class demonstrating how to subscribe to events.
 */
class ExampleListener {
    
    @OnRedisEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("Player ${event.playerName} joined server ${event.serverName}")
    }
    
    @OnRedisEvent
    fun onPlayerLeave(event: PlayerLeaveEvent) {
        println("Player ${event.playerName} left server ${event.serverName}")
    }
    
    @OnRedisEvent
    fun onChatMessage(event: ChatMessageEvent) {
        println("[${event.serverName}] ${event.playerName}: ${event.message}")
    }
}

/**
 * Example usage of the RedisEventBus with async/await
 */
fun main() = runBlocking {
    // Create event bus with Redis connection URI
    // Format: redis://password@host:port/database
    val redisApi = RedisApi.create(RedisURI.create("redis://localhost:6379"))

    // Register a listener
    val listener = ExampleListener()
    redisApi.subscribeToEvents(listener)

    // Freeze and connect to redis
    redisApi.freezeAndConnect()
    
    // Publish events asynchronously
    redisApi.publishEvent(PlayerJoinEvent("Steve", "uuid-123", "Lobby-1"))
    redisApi.publishEvent(ChatMessageEvent("Steve", "Hello World!", "Lobby-1"))
    redisApi.publishEvent(PlayerLeaveEvent("Steve", "uuid-123", "Lobby-1"))
    
    // Keep the application running to receive events
    println("Listening for events... Press Ctrl+C to exit")
    
    // Use a more graceful approach to keep the app running
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down...")
        redisApi.disconnect()
    })
    
    // Wait indefinitely (until Ctrl+C)
    Thread.currentThread().join()
}
