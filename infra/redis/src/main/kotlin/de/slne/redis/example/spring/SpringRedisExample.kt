package de.slne.redis.example.spring

import de.slne.redis.RedisApi
import de.slne.redis.event.RedisEventListener
import de.slne.redis.event.Subscribe
import de.slne.redis.example.PlayerJoinEvent
import de.slne.redis.example.ChatMessageEvent
import de.slne.redis.example.PlayerLeaveEvent
import de.slne.redis.request.RedisRequestHandler
import de.slne.redis.request.RequestContext
import kotlinx.coroutines.launch
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.ComponentScan

/**
 * Example Spring Boot application demonstrating surf-redis integration.
 * 
 * Features:
 * - Automatic Redis connection initialization via RedisApi
 * - Auto-discovery and registration of event listeners
 * - Auto-discovery and registration of request handlers
 * - Spring dependency injection
 */
@SpringBootApplication
@ComponentScan("de.slne.redis")
class SpringRedisExampleApp

fun main(args: Array<String>) {
    // Option 1: Initialize RedisApi before Spring starts (if needed)
    // RedisApi.init(url = "redis://localhost:6379")
    
    // Option 2: Let Spring auto-configuration handle it via application.properties
    // surf.redis.url=redis://localhost:6379
    
    SpringApplication.run(SpringRedisExampleApp::class.java, *args)
}

/**
 * Example event listener with auto-registration via Spring.
 * The @RedisEventListener annotation makes this a Spring component
 * that is automatically registered with the event bus.
 */
@RedisEventListener
class SpringPlayerEventListener {
    
    @Subscribe
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("[Spring Listener] Player ${event.playerName} joined server ${event.serverName}")
    }
    
    @Subscribe
    fun onPlayerLeave(event: PlayerLeaveEvent) {
        println("[Spring Listener] Player ${event.playerName} left server ${event.serverName}")
    }
    
    @Subscribe
    fun onChatMessage(event: ChatMessageEvent) {
        println("[Spring Listener] [${event.serverName}] ${event.playerName}: ${event.message}")
    }
}

/**
 * Example request handler with auto-registration via Spring.
 * The @RedisRequestHandler annotation makes this a Spring component
 * that is automatically registered with the request-response bus.
 */
@RedisRequestHandler
class SpringServerRequestHandler {
    
    @de.slne.redis.request.RequestHandler
    fun handleServerStatusRequest(context: RequestContext<de.slne.redis.example.request.ServerStatusRequest>) {
        context.coroutineScope.launch {
            // Simulate fetching server status
            val response = de.slne.redis.example.request.ServerStatusResponse(
                serverName = context.request.serverName,
                online = true,
                playerCount = 42
            )
            context.respond(response)
            println("[Spring Handler] Responded to server status request for ${context.request.serverName}")
        }
    }
}
