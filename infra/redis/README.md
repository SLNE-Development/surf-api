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
    implementation("de.slne:surf-redis:1.0.0")
}
```

## Usage

### 1. Create Custom Events

Create your custom events by extending the `RedisEvent` class and annotating with `@Serializable`:

```kotlin
import de.slne.redis.event.RedisEvent
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
import de.slne.redis.event.Subscribe

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
import de.slne.redis.event.RedisEventBus
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

## Example

See the `de.slne.redis.example` package for complete examples:
- `ExampleEvents.kt` - Example event definitions
- `ExampleUsage.kt` - Example usage demonstrating async publishing and subscribing

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

## License

This project is open source and available under the MIT License.