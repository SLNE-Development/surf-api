package de.slne.redis.example

import de.slne.redis.RedisApi
import de.slne.redis.event.RedisEventBus
import de.slne.redis.event.OnRedisEvent
import de.slne.redis.stream.RedisStreamEventBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

/**
 * Example demonstrating the new features:
 * 1. Global RedisApi connection
 * 2. Redis Streams support
 * 3. Direct usage without Spring
 */

fun main() = runBlocking {
    println("=== surf-redis Feature Demo ===\n")
    
    // Feature 1: Global Redis Connection via RedisApi
    println("1. Initializing global Redis connection via RedisApi")
    RedisApi.init(url = "redis://localhost:6379")
    println("   Redis connected: ${RedisApi.isConnected()}")
    println("   Redis URL: ${RedisApi.getUrl()}\n")
    
    // Alternative syntax as requested
    // RedisApi(url = "redis://localhost:6379").connect()
    
    // Feature 2: Traditional Event Bus (pub/sub)
    println("2. Using traditional RedisEventBus (pub/sub)")
    val eventBus = RedisEventBus("redis://localhost:6379")
    val listener1 = DemoListener("Listener1")
    eventBus.registerListener(listener1)
    
    eventBus.publish(PlayerJoinEvent("Alice", "uuid-1", "Server-1"))
    delay(100) // Give time for async handling
    println()
    
    // Feature 3: Redis Streams for reliable delivery
    println("3. Using RedisStreamEventBus (Redis Streams)")
    val streamBus = RedisStreamEventBus(
        streamName = "demo-events",
        consumerGroup = "demo-group",
        consumerName = "demo-consumer-1"
    )
    val listener2 = DemoListener("StreamListener")
    streamBus.registerListener(listener2)
    
    // Give time for consumer to start
    delay(500)
    
    streamBus.publish(PlayerJoinEvent("Bob", "uuid-2", "Server-2"))
    delay(100) // Give time for async handling
    println()
    
    // Feature 4: Multiple events
    println("4. Publishing multiple events")
    eventBus.publish(ChatMessageEvent("Alice", "Hello everyone!", "Server-1"))
    streamBus.publish(ChatMessageEvent("Bob", "Hi Alice!", "Server-2"))
    delay(100)
    println()
    
    // Clean up
    println("5. Cleaning up...")
    eventBus.close()
    streamBus.close()
    RedisApi.disconnect()
    println("Demo completed successfully!")
}

class DemoListener(private val name: String) {
    @OnRedisEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("   [$name] Player ${event.playerName} joined ${event.serverName}")
    }
    
    @OnRedisEvent
    fun onChatMessage(event: ChatMessageEvent) {
        println("   [$name] [${event.serverName}] ${event.playerName}: ${event.message}")
    }
}
