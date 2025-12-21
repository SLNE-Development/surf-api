package de.slne.redis.example

import de.slne.redis.event.RedisEventBus
import de.slne.redis.event.Subscribe

/**
 * Example listener class demonstrating how to subscribe to events.
 */
class ExampleListener {
    
    @Subscribe
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("Player ${event.playerName} joined server ${event.serverName}")
    }
    
    @Subscribe
    fun onPlayerLeave(event: PlayerLeaveEvent) {
        println("Player ${event.playerName} left server ${event.serverName}")
    }
    
    @Subscribe
    fun onChatMessage(event: ChatMessageEvent) {
        println("[${event.serverName}] ${event.playerName}: ${event.message}")
    }
}

/**
 * Example usage of the RedisEventBus
 */
fun main() {
    // Create event bus with Redis connection URI
    // Format: redis://password@host:port/database
    val eventBus = RedisEventBus("redis://localhost:6379")
    
    // Register a listener
    val listener = ExampleListener()
    eventBus.registerListener(listener)
    
    // Publish events
    eventBus.publish(PlayerJoinEvent("Steve", "uuid-123", "Lobby-1"))
    eventBus.publish(ChatMessageEvent("Steve", "Hello World!", "Lobby-1"))
    eventBus.publish(PlayerLeaveEvent("Steve", "uuid-123", "Lobby-1"))
    
    // Keep the application running to receive events
    println("Listening for events... Press Ctrl+C to exit")
    
    // Use a more graceful approach to keep the app running
    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down...")
        eventBus.close()
    })
    
    // Wait indefinitely (until Ctrl+C)
    Thread.currentThread().join()
}
