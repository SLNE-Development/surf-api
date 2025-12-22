# surf-redis

A Kotlin library for Redis-based event distribution using Lettuce. This library provides a simple, powerful, and **asynchronous** way to publish and subscribe to events across multiple servers or instances.

## Quick Start

```kotlin
import kotlinx.coroutines.runBlocking

// 1. Create your custom event (must be @Serializable)
@Serializable
data class PlayerJoinEvent(val playerName: String) : RedisEvent()

// 2. Create a listener
class MyListener {
    @Subscribe
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("${event.playerName} joined!")
    }
}

// 3. Set up and use asynchronously
runBlocking {
    val eventBus = RedisEventBus("redis://localhost:6379")
    eventBus.registerListener(MyListener())
    eventBus.publish(PlayerJoinEvent("Steve")) // async
    // or eventBus.publishBlocking(PlayerJoinEvent("Steve")) // blocking
}
```

## Features

- 🚀 **Async-first** event system based on Redis pub/sub using Lettuce and Kotlin Coroutines
- 📡 Distribute events across multiple servers/listeners
- 🔌 Easy plugin integration with automatic method scanning
- 🎯 Annotation-based event subscription
- 🔧 Type-safe event handling with Kotlin Serialization
- ⚡ High-performance event invocation using MethodHandles
- 🔄 **Redis Streams support** for reliable event delivery with persistence
- 🎛️ **Global Redis connection** management via RedisApi

## Requirements

- Kotlin 1.9.22 or higher
- Java 17 or higher
- Redis server

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation("dev.slne:surf-redis:1.0.0")
}
```

## Usage

### 1. Create Custom Events

Create your custom events by extending the `RedisEvent` class and annotating with `@Serializable`:

```kotlin
import dev.slne.redis.event.RedisEvent
import kotlinx.serialization.Serializable

@Serializable
data class PlayerJoinEvent(
    val playerName: String,
    val playerId: String,
    val serverName: String
) : RedisEvent()
```

### 2. Create Event Listeners

Create listeners with methods annotated with `@Subscribe`:

```kotlin
import dev.slne.redis.event.Subscribe

class MyListener {
    @Subscribe
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("Player ${event.playerName} joined!")
    }
}
```

### 3. Set Up the Event Bus

Initialize the event bus and register your listeners:

```kotlin
import dev.slne.redis.event.RedisEventBus
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // Connect to Redis
    val eventBus = RedisEventBus("redis://localhost:6379")
    
    // Register listeners
    val listener = MyListener()
    eventBus.registerListener(listener)
    
    // Publish events asynchronously
    eventBus.publish(PlayerJoinEvent("Steve", "uuid-123", "Lobby-1"))
    
    // Or use blocking variant for synchronous code
    eventBus.publishBlocking(PlayerJoinEvent("Alex", "uuid-456", "Lobby-2"))
    
    // Clean up when done
    eventBus.close()
}
```

### Redis Connection URI Format

The Redis URI follows this format:
```
redis://[password@]host[:port][/database]
```

Examples:
- `redis://localhost:6379` - Local Redis without password
- `redis://password@localhost:6379` - Local Redis with password
- `redis://mypassword@redis.example.com:6379/0` - Remote Redis with password and database

## Global Redis Connection (RedisApi)

surf-redis provides a centralized way to manage Redis connections via the `RedisApi` singleton:

```kotlin
import dev.slne.redis.RedisApi

// Initialize global connection
RedisApi.init(url = "redis://localhost:6379")

// Alternative syntax
RedisApi(url = "redis://localhost:6379").connect()

// Check connection status
if (RedisApi.isConnected()) {
    println("Connected to: ${RedisApi.getUrl()}")
}

// Create connections
val connection = RedisApi.createConnection()
val pubSubConnection = RedisApi.createPubSubConnection()

// Close connection when done
RedisApi.disconnect()
```

The `RedisApi` automatically initializes with a default URL (`redis://localhost:6379`) if not explicitly configured.

## Redis Streams

For more reliable event delivery with message persistence, use `RedisStreamEventBus`:

### Benefits of Redis Streams

- 📦 **Message Persistence**: Events are stored and not lost if no consumer is online
- 👥 **Consumer Groups**: Multiple instances can share the load
- ✅ **Message Acknowledgment**: Ensures events are processed
- 🔄 **Reprocessing**: Failed events can be reprocessed

### Usage

```kotlin
import dev.slne.redis.stream.RedisStreamEventBus

// Create stream-based event bus
val streamBus = RedisStreamEventBus(
    streamName = "my-events",
    consumerGroup = "my-app",
    consumerName = "instance-1"
)

// Use exactly like RedisEventBus
streamBus.registerListener(MyListener())
streamBus.publish(MyEvent())
streamBus.close()
```

Redis Streams provide stronger guarantees than pub/sub, making them ideal for critical events that must not be lost.

## How It Works

1. **Event Publishing**: When you call `eventBus.publish(event)`, the event is serialized using Kotlin Serialization and published asynchronously to a Redis channel using coroutines.

2. **Event Distribution**: All connected servers/instances subscribed to the same Redis channel receive the event asynchronously.

3. **Event Handling**: The event bus automatically deserializes the event using Kotlin Serialization and invokes all registered methods asynchronously in separate coroutines.

4. **Automatic Scanning**: When you call `registerListener()`, the event bus automatically scans the object for methods with the `@Subscribe` annotation and registers them using MethodHandles for optimal performance.

## Performance Optimizations

- **Async by default**: All event publishing and handling is asynchronous using Kotlin Coroutines
- **MethodHandles**: Uses Java MethodHandles instead of reflection for faster method invocation
- **Kotlin Serialization**: Uses Kotlin's native serialization instead of Gson for better performance and type safety

## Plugin Integration

For external plugins, simply call `registerListener()` with your plugin's listener object:

```kotlin
// In your plugin
class MyPlugin {
    fun enable(eventBus: RedisEventBus) {
        val listener = MyPluginListener()
        eventBus.registerListener(listener)
    }
}

@Serializable
data class CustomEvent(val data: String) : RedisEvent()

class MyPluginListener {
    @Subscribe
    fun onCustomEvent(event: CustomEvent) {
        // Handle event asynchronously
    }
}
```

## Request-Response Pattern

In addition to the event bus, surf-redis supports request-response patterns where a server can send a request and wait for a response with timeout support.

### Quick Start

```kotlin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import dev.slne.redis.request.*

// 1. Create your request and response (must be @Serializable)
@Serializable
data class GetPlayerRequest(val minLevel: Int) : RedisRequest()

@Serializable
data class PlayerListResponse(val players: List<String>) : RedisResponse()

// 2. Create a request handler
class PlayerRequestHandler {
    @RequestHandler
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine to respond asynchronously
        context.coroutineScope.launch {
            val players = fetchPlayersAsync(context.request.minLevel)
            context.respond(PlayerListResponse(players))
        }
    }
}

// 3. Set up and use
runBlocking {
    val bus = RequestResponseBus("redis://localhost:6379")
    
    // Register handler (ServerA)
    bus.registerRequestHandler(PlayerRequestHandler())
    
    // Send request and wait for response (ServerB or same server)
    val response = bus.sendRequest<PlayerListResponse>(
        GetPlayerRequest(minLevel = 5),
        timeoutMs = 3000 // Default timeout is 3 seconds
    )
    println("Players: ${response.players}")
}
```

### Features

- 🔄 **Request-Response Pattern**: Send requests and receive typed responses
- ⏱️ **Timeout Support**: Configurable timeout (default 3 seconds)
- 🔀 **Bidirectional**: Any server can both send requests and respond to requests
- 🚀 **Flexible Async**: Handlers control when to launch coroutines (not forced to be suspend)
- 🎯 **Type-safe**: Request and response types are validated at compile time

### Usage

#### 1. Create Request and Response Classes

```kotlin
import dev.slne.redis.request.RedisRequest
import dev.slne.redis.request.RedisResponse
import kotlinx.serialization.Serializable

@Serializable
data class GetPlayerRequest(val minLevel: Int) : RedisRequest()

@Serializable
data class PlayerListResponse(val players: List<String>) : RedisResponse()
```

#### 2. Create Request Handlers

Handlers receive a `RequestContext` that provides:
- `request`: The incoming request
- `coroutineScope`: Scope for launching coroutines if needed
- `respond(response)`: Method to send the response

```kotlin
import dev.slne.redis.request.RequestHandler
import dev.slne.redis.request.RequestContext
import kotlinx.coroutines.launch

class MyRequestHandler {
    @RequestHandler
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine for async operations
        context.coroutineScope.launch {
            val players = fetchPlayersFromDatabaseAsync(context.request.minLevel)
            context.respond(PlayerListResponse(players))
        }
    }
}
```

#### 3. Register Handlers and Send Requests

```kotlin
import dev.slne.redis.request.RequestResponseBus
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    val bus = RequestResponseBus("redis://localhost:6379")
    
    // Register handler (this server will respond to requests)
    bus.registerRequestHandler(MyRequestHandler())
    
    // Send request and wait for response (with timeout)
    try {
        val response = bus.sendRequest<PlayerListResponse>(
            GetPlayerRequest(minLevel = 10),
            timeoutMs = 3000
        )
        println("Received ${response.players.size} players")
    } catch (e: RequestTimeoutException) {
        println("Request timed out: ${e.message}")
    }
    
    bus.close()
}
```

### Request-Response vs Events

**Use Request-Response when:**
- You need a reply/acknowledgment
- You need data back from another server
- You need to know if the operation succeeded
- You want timeout handling

**Use Events when:**
- You want to broadcast information
- You don't need a reply
- Multiple servers should react to the same event
- Fire-and-forget pattern is acceptable

### Example Scenario

**ServerA** (Lobby Server) and **ServerB** (Game Server):

```kotlin
// ServerA can handle requests
class LobbyHandler {
    @RequestHandler
    fun getServerStatus(context: RequestContext<ServerStatusRequest>) {
        // Respond using coroutine scope
        context.coroutineScope.launch {
            context.respond(ServerStatusResponse("Lobby-1", online = true, playerCount = 42))
        }
    }
}

// ServerB can also send requests to ServerA
val response = bus.sendRequest<ServerStatusResponse>(
    ServerStatusRequest("Lobby-1"),
    timeoutMs = 3000
)
```

Both servers can simultaneously:
- Send requests to other servers
- Respond to requests from other servers

## Example

See the `dev.slne.redis.example` package for complete examples:
- `ExampleEvents.kt` - Example event definitions
- `ExampleUsage.kt` - Example usage demonstrating async publishing and subscribing
- `ExampleRequests.kt` - Example request/response definitions
- `ExampleRequestResponseUsage.kt` - Example usage demonstrating request-response pattern

## API Reference

### RedisEvent

Base class for all events. Extend this to create custom events. Must be annotated with `@Serializable`.

### @Subscribe

Annotation for marking event handler methods. Methods must:
- Have exactly one parameter
- The parameter must be a subclass of `RedisEvent`
- Handler will be invoked asynchronously in a coroutine

### RedisEventBus

Main class for managing events with async support.

**Constructor:**
- `RedisEventBus(redisUri: String, coroutineScope: CoroutineScope = ...)` - Create a new event bus connected to Redis

**Methods:**
- `suspend fun publish(event: RedisEvent)` - Publish an event asynchronously to all listeners
- `fun publishBlocking(event: RedisEvent)` - Publish an event synchronously (blocking)
- `fun registerListener(listener: Any)` - Register an object with @Subscribe methods (uses MethodHandles)
- `fun unregisterListener(listener: Any)` - Unregister a listener and all its handlers
- `fun close()` - Close connections and clean up resources

### RedisRequest

Base class for all requests. Extend this to create custom requests. Must be annotated with `@Serializable`.

### RedisResponse

Base class for all responses. Extend this to create custom responses. Must be annotated with `@Serializable`.

### @RequestHandler

Annotation for marking request handler methods. Methods must:
- Have exactly one parameter of type `RequestContext<TRequest>`
- Return void (Unit)
- Handler controls when/how to respond using `context.respond()`

### RequestContext<TRequest>

Context object provided to request handlers containing:
- `request: TRequest` - The incoming request
- `coroutineScope: CoroutineScope` - Scope for launching coroutines if needed
- `suspend fun respond(response: RedisResponse)` - Send the response

### RequestResponseBus

Main class for managing request-response patterns with async support and timeout handling.

**Constructor:**
- `RequestResponseBus(redisUri: String, coroutineScope: CoroutineScope = ...)` - Create a new request-response bus connected to Redis

**Methods:**
- `suspend fun <T : RedisResponse> sendRequest(request: RedisRequest, timeoutMs: Long = 3000): T` - Send a request and wait for response asynchronously
- `fun <T : RedisResponse> sendRequestBlocking(request: RedisRequest, timeoutMs: Long = 3000): T` - Send a request and wait for response synchronously (blocking)
- `fun registerRequestHandler(handler: Any)` - Register an object with @RequestHandler methods
- `fun unregisterRequestHandler(handler: Any)` - Unregister a handler and all its request handlers
- `fun close()` - Close connections and clean up resources

### RequestTimeoutException

Exception thrown when a request times out without receiving a response.

## License

This project is open source and available under the MIT License.