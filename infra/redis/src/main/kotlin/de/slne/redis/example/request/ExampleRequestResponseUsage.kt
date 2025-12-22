package de.slne.redis.example.request

import de.slne.redis.RedisApi
import de.slne.redis.request.HandleRedisRequest
import de.slne.redis.request.RequestContext
import de.slne.redis.request.RequestResponseBus
import io.lettuce.core.RedisURI
import kotlinx.coroutines.*

/**
 * Example request handler demonstrating how to handle requests and send responses.
 */
class ExampleRequestHandler {
    private val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    @HandleRedisRequest
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine to respond asynchronously (if needed)
        coroutineScope.launch {
            // Simulate some async work (e.g., database query)
            delay(100)

            // Filter players based on minimum level
            val allPlayers = listOf("Steve", "Alex", "Notch", "Herobrine", "Jeb")
            val filteredPlayers = if (context.request.minLevel > 5) {
                allPlayers.take(2)  // Only high-level players
            } else {
                allPlayers
            }

            context.respond(PlayerListResponse(filteredPlayers))
        }
    }

    @HandleRedisRequest
    fun handleServerStatus(context: RequestContext<ServerStatusRequest>) {
        // Respond synchronously by launching in runBlocking or use coroutineScope.launch for async
        coroutineScope.launch {
            // Simulate checking server status
            delay(50)

            context.respond(
                ServerStatusResponse(
                    serverName = context.request.serverName,
                    online = true,
                    playerCount = 42
                )
            )
        }
    }
}

/**
 * Example usage of the RequestResponseBus demonstrating request/response pattern
 */
fun main() = runBlocking {
    // Create RedisApi instance
    val redisApi = RedisApi.create(RedisURI.create("redis://localhost:6379"))

    // Register a request handler (this is the server that responds to requests)
    val handler = ExampleRequestHandler()
    redisApi.registerRequestHandler(handler)

    // Freeze and connect to Redis
    redisApi.freezeAndConnect()

    println("Server ready - listening for requests...")

    // Simulate a client sending requests after a short delay
    delay(500)

    try {
        // Send a request and wait for response
        println("Sending GetPlayerRequest with minLevel=5...")
        val response1 = redisApi.sendRequest<PlayerListResponse>(
            GetPlayerRequest(minLevel = 5),
            timeoutMs = 3000
        )
        println("Received response: ${response1.players}")

        // Send another request
        println("\nSending ServerStatusRequest...")
        val response2 = redisApi.sendRequest<ServerStatusResponse>(
            ServerStatusRequest("Lobby-1"),
            timeoutMs = 3000
        )
        println("Server status: ${response2.serverName} - Online: ${response2.online}, Players: ${response2.playerCount}")

    } catch (e: Exception) {
        println("Error: ${e.message}")
        e.printStackTrace()
    }

    // Keep the application running to handle more requests
    println("\nPress Ctrl+C to exit")

    Runtime.getRuntime().addShutdownHook(Thread {
        println("Shutting down...")
        redisApi.disconnect()
    })

    // Wait indefinitely
    Thread.currentThread().join()
}
