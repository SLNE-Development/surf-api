# surf-redis

A Kotlin library for Redis-based event distribution using Lettuce. This library provides a simple and powerful way to publish and subscribe to events across multiple servers or instances.

## Features

- 🚀 Simple event system based on Redis pub/sub using Lettuce
- 📡 Distribute events across multiple servers/listeners
- 🔌 Easy plugin integration with automatic method scanning
- 🎯 Annotation-based event subscription
- 🔧 Type-safe event handling

## Requirements

- Kotlin 1.9.22 or higher
- Java 17 or higher
- Redis server

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("de.slne:surf-redis:1.0.0")
}
```

## Usage

### 1. Create Custom Events

Create your custom events by extending the `RedisEvent` class:

```kotlin
import de.slne.redis.event.RedisEvent

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

fun main() {
    // Connect to Redis
    val eventBus = RedisEventBus("redis://localhost:6379")
    
    // Register listeners
    val listener = MyListener()
    eventBus.registerListener(listener)
    
    // Publish events
    eventBus.publish(PlayerJoinEvent("Steve", "uuid-123", "Lobby-1"))
    
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

1. **Event Publishing**: When you call `eventBus.publish(event)`, the event is serialized to JSON and published to a Redis channel.

2. **Event Distribution**: All connected servers/instances subscribed to the same Redis channel receive the event.

3. **Event Handling**: The event bus automatically deserializes the event and invokes all registered methods that have the `@Subscribe` annotation and accept that event type as a parameter.

4. **Automatic Scanning**: When you call `registerListener()`, the event bus automatically scans the object for methods with the `@Subscribe` annotation and registers them.

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

class MyPluginListener {
    @Subscribe
    fun onCustomEvent(event: CustomEvent) {
        // Handle event
    }
}
```

## Example

See the `de.slne.redis.example` package for complete examples:
- `ExampleEvents.kt` - Example event definitions
- `ExampleUsage.kt` - Example usage demonstrating publishing and subscribing

## API Reference

### RedisEvent

Base class for all events. Extend this to create custom events.

### @Subscribe

Annotation for marking event handler methods. Methods must:
- Have exactly one parameter
- The parameter must be a subclass of `RedisEvent`

### RedisEventBus

Main class for managing events.

**Constructor:**
- `RedisEventBus(redisUri: String)` - Create a new event bus connected to Redis

**Methods:**
- `publish(event: RedisEvent)` - Publish an event to all listeners
- `registerListener(listener: Any)` - Register an object with @Subscribe methods
- `unregisterListener(listener: Any)` - Unregister a listener and all its handlers
- `close()` - Close connections and clean up resources

## License

This project is open source and available under the MIT License.