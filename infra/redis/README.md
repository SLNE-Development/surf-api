# surf-redis

A Kotlin library for Redis-based distributed systems using Lettuce and Kotlin Coroutines. This library provides a comprehensive, type-safe, and **asynchronous** solution for event distribution, request-response patterns, and synchronized data structures across multiple servers or instances.

## Quick Start

```kotlin
import dev.slne.redis.RedisApi
import dev.slne.redis.event.RedisEvent
import dev.slne.redis.event.OnRedisEvent
import kotlinx.serialization.Serializable
import java.nio.file.Paths

// 1. Create your custom event (must be @Serializable)
@Serializable
data class PlayerJoinEvent(val playerName: String) : RedisEvent()

// 2. Create a listener
class MyListener {
    @OnRedisEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("${event.playerName} joined!")
    }
}

// 3. Set up using path-based configuration (RECOMMENDED)
val api = RedisApi.create(
    pluginDataPath = Paths.get("plugins/my-plugin"),
    pluginsPath = Paths.get("plugins")
)

// 4. Register listeners before freezing
api.subscribeToEvents(MyListener())

// 5. Freeze and connect
api.freezeAndConnect()

// 6. Publish events
api.publishEvent(PlayerJoinEvent("Steve"))
```

## Features

- 🚀 **Async-first** architecture based on Redis Pub/Sub using Lettuce and Kotlin Coroutines
- 📡 **Event Bus**: Distribute events across multiple servers/instances
- 🔄 **Request-Response**: Send requests and receive typed responses with timeout support
- 🔗 **Synchronized Data Structures**: Replicated in-memory collections (List, Map, Set, Value)
- 🎛️ **Centralized Configuration**: Global Redis config shared across all plugins
- 🔌 Easy plugin integration with automatic method scanning
- 🎯 Annotation-based event and request handlers
- 🔧 Type-safe handling with Kotlin Serialization
- ⚡ High-performance invocation using Java's LambdaMetafactory
- 🛡️ Thread-safe with proper locking and coroutine support

## Requirements

- Kotlin 1.9.22 or higher
- Java 21 or higher
- Redis server (tested with Redis 7.x)

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
plugins {
    kotlin("plugin.serialization") version "1.9.22"
}

dependencies {
    implementation("dev.slne:surf-redis:1.0.0-SNAPSHOT")
}
```

## Core Concepts

### RedisApi - Central Connection Manager

`RedisApi` is the main entry point for all Redis functionality. It manages:
- Redis connections (command and Pub/Sub)
- Event bus for publishing and subscribing to events
- Request-response bus for bi-directional communication
- Synchronized data structures (replicated collections)

### Two-Phase Initialization

surf-redis uses a two-phase initialization pattern to ensure all features are registered before connecting:

1. **Registration Phase**: Create the API and register features (events, requests, sync structures)
2. **Freeze**: Call `freeze()` to lock registrations
3. **Connect**: Call `connect()` to establish Redis connections

Or use the convenience method `freezeAndConnect()` to do steps 2 and 3 together.

```kotlin
val api = RedisApi.create(...)

// Registration phase
api.subscribeToEvents(myListener)
api.registerRequestHandler(myHandler)
val syncList = api.createSyncList<String>("my-list")

// Freeze and connect
api.freezeAndConnect()

// Now ready to use
api.publishEvent(MyEvent())
```

## Creating a RedisApi Instance

### Path-Based Configuration (RECOMMENDED)

**This is the preferred method** because it enables centralized configuration management:

```kotlin
import dev.slne.redis.RedisApi
import java.nio.file.Paths

val api = RedisApi.create(
    pluginDataPath = Paths.get("plugins/my-plugin"),
    pluginsPath = Paths.get("plugins")  // optional, defaults to parent of pluginDataPath
)
```

#### How It Works

When you create a `RedisApi` using paths:

1. **Local Configuration**: A `redis.yml` file is created in your plugin's data directory (e.g., `plugins/my-plugin/redis.yml`):
   ```yaml
   useGlobalConfig: true  # defaults to true
   local:
     host: localhost
     port: 6379
   ```

2. **Global Configuration**: If `useGlobalConfig` is `true` (default), a shared `global.yml` is created in `plugins/surf-redis/`:
   ```yaml
   host: localhost
   port: 6379
   ```

3. **Benefits**:
   - **Server owners** can configure Redis once in `global.yml` instead of per plugin
   - Easier to manage when multiple plugins use surf-redis
   - Future Redis options can be added globally
   - Individual plugins can still override by setting `useGlobalConfig: false`

### RedisURI-Based Configuration (Alternative)

For more control or when paths aren't available:

```kotlin
import io.lettuce.core.RedisURI

val api = RedisApi.create(
    redisURI = RedisURI.create("redis://localhost:6379")
)
```

Redis URI format:
```
redis://[password@]host[:port][/database]
```

Examples:
- `redis://localhost:6379` - Local Redis without password
- `redis://password@localhost:6379` - With password
- `redis://mypassword@redis.example.com:6379/0` - Remote with password and database

## Usage

### Event Bus

The event bus allows you to publish events to all listening servers/instances.

#### 1. Create Custom Events

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

#### 2. Create Event Listeners

Create listeners with methods annotated with `@OnRedisEvent`:

```kotlin
import dev.slne.redis.event.OnRedisEvent

class MyListener {
    @OnRedisEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("Player ${event.playerName} joined on ${event.serverName}!")
    }
}
```

**Important**: Event handlers are invoked on the Redis Pub/Sub thread. Keep them fast and launch coroutines for heavy work:

```kotlin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyListener(private val scope: CoroutineScope) {
    @OnRedisEvent
    fun onPlayerJoinAsync(event: PlayerJoinEvent) {
        // Launch coroutine for async/heavy work
        scope.launch(Dispatchers.IO) {
            val data = fetchPlayerDataFromDatabase(event.playerId)
            processPlayerJoin(data)
        }
    }
}
```

#### 3. Register and Use

```kotlin
import dev.slne.redis.RedisApi
import java.nio.file.Paths

// Create and configure API
val api = RedisApi.create(
    pluginDataPath = Paths.get("plugins/my-plugin")
)

// Register listeners BEFORE freezing
api.subscribeToEvents(MyListener())

// Freeze and connect
api.freezeAndConnect()

// Publish events
api.publishEvent(PlayerJoinEvent("Steve", "uuid-123", "Lobby-1"))

// Clean up when done
api.disconnect()
```

### Request-Response Pattern

In addition to events, surf-redis supports request-response patterns where a server can send a request and wait for a response with timeout support.

#### Quick Example

```kotlin
import dev.slne.redis.RedisApi
import dev.slne.redis.request.*
import kotlinx.serialization.Serializable
import kotlinx.coroutines.launch
import java.nio.file.Paths

// 1. Create request and response (must be @Serializable)
@Serializable
data class GetPlayerRequest(val minLevel: Int) : RedisRequest()

@Serializable
data class PlayerListResponse(val players: List<String>) : RedisResponse()

// 2. Create a request handler
class PlayerRequestHandler {
    @HandleRedisRequest
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine to respond asynchronously
        context.coroutineScope.launch {
            val players = fetchPlayersAsync(context.request.minLevel)
            context.respond(PlayerListResponse(players))
        }
    }
}

// 3. Set up and use
val api = RedisApi.create(pluginDataPath = Paths.get("plugins/my-plugin"))

// Register handler (ServerA)
api.registerRequestHandler(PlayerRequestHandler())

api.freezeAndConnect()

// Send request and wait for response (ServerB or same server)
val response = api.sendRequest<PlayerListResponse>(
    GetPlayerRequest(minLevel = 5),
    timeoutMs = 5000  // Default timeout is 5 seconds
)
println("Players: ${response.players}")
```

#### Features

- 🔄 **Request-Response Pattern**: Send requests and receive typed responses
- ⏱️ **Timeout Support**: Configurable timeout (default 5 seconds)
- 🔀 **Bidirectional**: Any server can both send requests and respond to requests
- 🚀 **Flexible Async**: Handlers control when to launch coroutines
- 🎯 **Type-safe**: Request and response types validated at compile time

#### Creating Requests and Responses

```kotlin
import dev.slne.redis.request.RedisRequest
import dev.slne.redis.request.RedisResponse
import kotlinx.serialization.Serializable

@Serializable
data class GetPlayerRequest(val minLevel: Int) : RedisRequest()

@Serializable
data class PlayerListResponse(val players: List<String>) : RedisResponse()
```

#### Creating Request Handlers

Handlers receive a `RequestContext` that provides:
- `request`: The incoming request
- `coroutineScope`: Scope for launching coroutines if needed
- `respond(response)`: Method to send the response

```kotlin
import dev.slne.redis.request.HandleRedisRequest
import dev.slne.redis.request.RequestContext
import kotlinx.coroutines.launch

class MyRequestHandler {
    @HandleRedisRequest
    fun handlePlayerRequest(context: RequestContext<GetPlayerRequest>) {
        // Launch coroutine for async operations
        context.coroutineScope.launch {
            val players = fetchPlayersFromDatabaseAsync(context.request.minLevel)
            context.respond(PlayerListResponse(players))
        }
    }
}
```

**Important**: Request handlers are invoked on the Redis Pub/Sub thread, just like event handlers. Launch coroutines for async work.

#### Sending Requests

```kotlin
import dev.slne.redis.RedisApi
import kotlinx.coroutines.runBlocking

// Example only - in production, use a proper coroutine scope
runBlocking {
    val api = RedisApi.create(...)
    api.registerRequestHandler(MyRequestHandler())
    api.freezeAndConnect()
    
    // Send request and wait for response (with timeout)
    try {
        val response = api.sendRequest<PlayerListResponse>(
            GetPlayerRequest(minLevel = 10),
            timeoutMs = 5000
        )
        println("Received ${response.players.size} players")
    } catch (e: RequestTimeoutException) {
        println("Request timed out: ${e.message}")
    }
}
```

#### When to Use Request-Response vs Events

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

#### Example: Cross-Server Communication

**ServerA** (Lobby Server) and **ServerB** (Game Server):

```kotlin
// ServerA: Handle status requests
class LobbyHandler {
    @HandleRedisRequest
    fun getServerStatus(context: RequestContext<ServerStatusRequest>) {
        context.coroutineScope.launch {
            context.respond(
                ServerStatusResponse(
                    serverName = "Lobby-1",
                    online = true,
                    playerCount = 42
                )
            )
        }
    }
}

// ServerB: Query ServerA
val response = api.sendRequest<ServerStatusResponse>(
    ServerStatusRequest("Lobby-1"),
    timeoutMs = 5000
)
println("Lobby-1 has ${response.playerCount} players")
```

Both servers can simultaneously send requests to and respond to requests from other servers.

### Synchronized Data Structures

surf-redis provides replicated, in-memory data structures that stay synchronized across all Redis-connected nodes.

#### Overview

- **SyncList<T>**: Replicated list
- **SyncMap<K, V>**: Replicated map
- **SyncSet<T>**: Replicated set
- **SyncValue<T>**: Replicated single value

#### How They Work

- **In-memory state**: Each node maintains its own local copy
- **Delta replication**: Mutations are broadcast via Redis Pub/Sub
- **Snapshot for late joiners**: Full state is stored in Redis with TTL
- **Versioning**: Each mutation increments a version; gaps trigger resync
- **Eventual consistency**: Changes propagate asynchronously
- **Thread-safe**: Protected with read-write locks
- **Change listeners**: React to local and remote changes

#### Quick Example

```kotlin
import dev.slne.redis.RedisApi
import java.nio.file.Paths

val api = RedisApi.create(pluginDataPath = Paths.get("plugins/my-plugin"))

// Create synchronized list (BEFORE freezing)
val playerList = api.createSyncList<String>("online-players")

// Optional: Listen for changes
playerList.onChange { change ->
    when (change) {
        is SyncList.Change.Add -> println("Player added: ${change.element}")
        is SyncList.Change.Remove -> println("Player removed: ${change.element}")
        is SyncList.Change.Set -> println("Player changed")
        is SyncList.Change.Clear -> println("List cleared")
    }
}

api.freezeAndConnect()

// Use like a regular list - changes replicate automatically
playerList.add("Steve")        // All nodes see this
playerList.remove("Steve")     // All nodes see this
playerList.clear()             // All nodes see this

val players = playerList.toList()  // Get local snapshot
```

#### SyncList Example

```kotlin
// Create
val list = api.createSyncList<String>(
    id = "my-list",
    ttl = Duration.minutes(10)  // Optional, default is 5 minutes
)

api.freezeAndConnect()

// Operations (thread-safe, replicated)
list.add("item1")
list.add(1, "item2")
list[0] = "updated"
list.removeAt(1)
list.clear()

// Read operations
val size = list.size
val item = list[0]
val items = list.toList()
```

#### SyncMap Example

```kotlin
// Create
val map = api.createSyncMap<String, Int>(
    id = "player-scores",
    ttl = Duration.minutes(10)
)

api.freezeAndConnect()

// Operations (thread-safe, replicated)
map["player1"] = 100
map["player2"] = 200
map.remove("player1")
map.clear()

// Read operations
val score = map["player1"]
val keys = map.keys()
val values = map.values()
val entries = map.entries()
```

#### SyncSet Example

```kotlin
// Create
val set = api.createSyncSet<String>(
    id = "active-lobbies",
    ttl = Duration.minutes(10)
)

api.freezeAndConnect()

// Operations (thread-safe, replicated)
set.add("lobby1")
set.add("lobby2")
set.remove("lobby1")
set.clear()

// Read operations
val contains = set.contains("lobby1")
val size = set.size
val elements = set.toSet()
```

#### SyncValue Example

```kotlin
// Create
val value = api.createSyncValue(
    id = "maintenance-mode",
    defaultValue = false,
    ttl = Duration.minutes(10)
)

api.freezeAndConnect()

// Operations (thread-safe, replicated)
value.set(true)
val current = value.get()

// Listen for changes
value.onChange { change ->
    println("Maintenance mode: ${change.old} -> ${change.new}")
}
```

#### Important Notes

- **Create before freezing**: All sync structures must be created before calling `freeze()`
- **Thread-safe**: Safe to use from multiple threads
- **Async replication**: Changes propagate asynchronously via Pub/Sub
- **Eventual consistency**: Not strongly consistent - expect small delays
- **TTL management**: Structures auto-expire without active nodes; heartbeat keeps them alive
- **Late joiners**: New nodes load full snapshot, then receive deltas

## Complete Example

Here's a complete example demonstrating all features:

```kotlin
import dev.slne.redis.RedisApi
import dev.slne.redis.event.RedisEvent
import dev.slne.redis.event.OnRedisEvent
import dev.slne.redis.request.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import java.nio.file.Paths

// Events
@Serializable
data class PlayerJoinEvent(val name: String) : RedisEvent()

// Requests & Responses
@Serializable
data class GetPlayersRequest(val minLevel: Int) : RedisRequest()

@Serializable
data class PlayersResponse(val players: List<String>) : RedisResponse()

// Handlers
class GameHandlers {
    @OnRedisEvent
    fun onPlayerJoin(event: PlayerJoinEvent) {
        println("${event.name} joined!")
    }
    
    @HandleRedisRequest
    fun handleGetPlayers(ctx: RequestContext<GetPlayersRequest>) {
        ctx.coroutineScope.launch {
            val players = listOf("Steve", "Alex")
            ctx.respond(PlayersResponse(players))
        }
    }
}

fun main() {
    // Create API (path-based, RECOMMENDED)
    val api = RedisApi.create(
        pluginDataPath = Paths.get("plugins/my-plugin")
    )
    
    // Register features BEFORE freezing
    api.subscribeToEvents(GameHandlers())
    api.registerRequestHandler(GameHandlers())
    
    val playerList = api.createSyncList<String>("online-players")
    playerList.onChange { change ->
        println("Player list changed: $change")
    }
    
    // Freeze and connect
    api.freezeAndConnect()
    
    // Now ready to use
    api.publishEvent(PlayerJoinEvent("Steve"))
    
    playerList.add("Steve")
    playerList.add("Alex")
    
    // Send request (requires suspend context)
    // Note: In production, use a proper coroutine scope instead of runBlocking
    kotlinx.coroutines.runBlocking {
        val response = api.sendRequest<PlayersResponse>(
            GetPlayersRequest(minLevel = 5),
            timeoutMs = 5000
        )
        println("Players: ${response.players}")
    }
    
    // Clean up
    api.disconnect()
}
```

## API Reference

### RedisApi

Central API for managing Redis connections and features.

**Creation Methods:**
```kotlin
// Path-based (RECOMMENDED)
RedisApi.create(
    pluginDataPath: Path,
    pluginsPath: Path = pluginDataPath.parent,
    serializerModule: SerializersModule = EmptySerializersModule()
): RedisApi

// RedisURI-based
RedisApi.create(
    redisURI: RedisURI,
    serializerModule: SerializersModule = EmptySerializersModule()
): RedisApi
```

**Lifecycle Methods:**
- `freeze()` - Lock registrations, prepare for connection
- `connect()` - Connect to Redis (must be frozen first)
- `freezeAndConnect()` - Convenience method for freeze + connect
- `disconnect()` - Close connections and clean up
- `isFrozen(): Boolean` - Check if frozen
- `isConnected(): Boolean` - Check if connected
- `suspend fun isAlive(): Boolean` - Ping Redis

**Connection Info:**
- `getHost(): String` - Get Redis host
- `getPort(): Int` - Get Redis port

**Event Bus Methods:**
- `publishEvent(event: RedisEvent)` - Publish event to all listeners
- `subscribeToEvents(listener: Any)` - Register event listener (before freeze)

**Request-Response Methods:**
- `suspend fun <T : RedisResponse> sendRequest(request: RedisRequest, timeoutMs: Long = 5000): T`
- `registerRequestHandler(handler: Any)` - Register request handler (before freeze)

**Sync Structure Methods:**
- `createSyncList<E>(id: String, ttl: Duration = 5.minutes): SyncList<E>`
- `createSyncMap<K, V>(id: String, ttl: Duration = 5.minutes): SyncMap<K, V>`
- `createSyncSet<E>(id: String, ttl: Duration = 5.minutes): SyncSet<E>`
- `createSyncValue<T>(id: String, defaultValue: T, ttl: Duration = 5.minutes): SyncValue<T>`

### RedisEvent

Base class for all events. Extend this to create custom events. Must be annotated with `@Serializable`.

```kotlin
@Serializable
data class MyEvent(val data: String) : RedisEvent()
```

### @OnRedisEvent

Annotation for event handler methods. Methods must:
- Have exactly one parameter
- The parameter must be a subclass of `RedisEvent`
- **Not** be a `suspend` function
- Handler invoked on Pub/Sub thread (launch coroutines for async work)

```kotlin
@OnRedisEvent
fun onEvent(event: MyEvent) {
    // Handle event
}
```

### RedisRequest

Base class for all requests. Extend this to create custom requests. Must be annotated with `@Serializable`.

```kotlin
@Serializable
data class MyRequest(val query: String) : RedisRequest()
```

### RedisResponse

Base class for all responses. Extend this to create custom responses. Must be annotated with `@Serializable`.

```kotlin
@Serializable
data class MyResponse(val result: String) : RedisResponse()
```

### @HandleRedisRequest

Annotation for request handler methods. Methods must:
- Have exactly one parameter of type `RequestContext<T>`
- `T` must be a subclass of `RedisRequest`
- **Not** be a `suspend` function
- Return `Unit`
- Handler invoked on Pub/Sub thread (launch coroutines for async work)

```kotlin
@HandleRedisRequest
fun handleRequest(ctx: RequestContext<MyRequest>) {
    ctx.coroutineScope.launch {
        ctx.respond(MyResponse("result"))
    }
}
```

### RequestContext<TRequest>

Context object provided to request handlers.

**Properties:**
- `request: TRequest` - The incoming request
- `coroutineScope: CoroutineScope` - Scope for launching coroutines

**Methods:**
- `suspend fun respond(response: RedisResponse)` - Send the response (call exactly once)

### SyncList<T>, SyncMap<K, V>, SyncSet<T>, SyncValue<T>

Replicated in-memory data structures. See "Synchronized Data Structures" section for details.

**Common Features:**
- Thread-safe operations
- Automatic replication via Pub/Sub
- Change listeners
- Snapshot loading for late joiners
- TTL management

### RequestTimeoutException

Exception thrown when a request times out without receiving a response.

## Performance Considerations

- **Async by default**: All operations use Kotlin Coroutines
- **LambdaMetafactory**: Uses Java's LambdaMetafactory to generate optimized handler invocations (faster than reflection)
- **Kotlin Serialization**: Native, type-safe serialization
- **Thread safety**: Proper locking in sync structures
- **Pub/Sub thread**: Handlers invoked synchronously; launch coroutines for heavy work
- **Eventual consistency**: Sync structures are eventually consistent, not strongly consistent

## Best Practices

1. **Use path-based configuration**: Enables global config for server owners
2. **Create structures before freezing**: All registrations must happen before `freeze()`
3. **Launch coroutines in handlers**: Don't block the Pub/Sub thread
4. **Handle timeouts**: Request-response calls can time out
5. **Use change listeners**: React to remote changes in sync structures
6. **Clean up**: Call `disconnect()` on shutdown
7. **Choose the right pattern**:
   - Events for broadcasts
   - Request-response for data queries
   - Sync structures for shared state

## License

This project is open source and available under the MIT License.