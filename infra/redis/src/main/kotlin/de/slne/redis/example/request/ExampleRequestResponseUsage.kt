package de.slne.redis.example.request

import de.slne.redis.request.RequestContext
import de.slne.redis.request.RequestHandler
import de.slne.redis.request.RequestResponseBus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 * Example request handler demonstrating how to handle requests and send responses.
 */
class ExampleRequestHandler {
    
    @RequestHandler
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine to respond asynchronously (if needed)
        context.coroutineScope.launch {
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
    
    @RequestHandler
    fun handleServerStatus(context: RequestContext<ServerStatusRequest>) {
        // Respond synchronously by launching in runBlocking or use coroutineScope.launch for async
        context.coroutineScope.launch {
            // Simulate checking server status
            delay(50)
            
            context.respond(ServerStatusResponse(
                serverName = context.request.serverName,
                online = true,
                playerCount = 42
            ))
        }
    }
}

/**
 * Example usage of the RequestResponseBus demonstrating request/response pattern
 */
fun main() = runBlocking {
    // Create request-response bus with Redis connection URI
    val requestResponseBus = RequestResponseBus("redis://localhost:6379")
    
    // Register a request handler (this is the server that responds to requests)
    val handler = ExampleRequestHandler()
    requestResponseBus.registerRequestHandler(handler)
    
    println("Server ready - listening for requests...")
    
    // Simulate a client sending requests after a short delay
    delay(500)
    
    try {
        // Send a request and wait for response
        println("Sending GetPlayerRequest with minLevel=5...")
        val response1 = requestResponseBus.sendRequest<PlayerListResponse>(
            GetPlayerRequest(minLevel = 5),
            timeoutMs = 3000
        )
        println("Received response: ${response1.players}")
        
        // Send another request
        println("\nSending ServerStatusRequest...")
        val response2 = requestResponseBus.sendRequest<ServerStatusResponse>(
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
        requestResponseBus.close()
    })
    
    // Wait indefinitely
    Thread.currentThread().join()
}
