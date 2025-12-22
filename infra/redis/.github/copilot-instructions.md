# Copilot Instructions for surf-redis

## Project Overview
Kotlin library for Redis-based event distribution, request-response patterns, and synchronized data structures using Lettuce and Kotlin Coroutines. Provides async event pub/sub across distributed systems, request-response with timeout, and replicated in-memory structures (lists, maps, sets, values).

**Stack**: Kotlin 1.9.22+, Java 21, Gradle 9.1.0, Lettuce 7.2.1, 27 source files (~700KB)

## ⚠️ CRITICAL: Build Blocker
**Custom Gradle plugin `dev.slne.surf.surfapi.gradle.core:1.21.11+` is NOT publicly available.** All `./gradlew` commands FAIL with plugin resolution error. Build/test from scratch is IMPOSSIBLE without plugin access.

**Required**: Java 21 (`export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`), default is Java 17.

**Workaround**: Focus on code review, validate imports, check syntax, review patterns. DO NOT remove/replace plugin.

## Project Structure
```
src/main/kotlin/dev/slne/redis/
├── RedisApi.kt                    # Central connection management, two-phase init (freeze→connect)
├── event/                         # Pub/sub event bus
│   ├── RedisEventBus.kt          # Main implementation, MethodHandles dispatch
│   ├── RedisEvent.kt (abstract)  # Base for events, requires @Serializable
│   └── OnRedisEvent.kt           # Annotation for handlers
├── request/                       # Request-response pattern
│   ├── RequestResponseBus.kt     # Correlation-based req/resp, 5s timeout default
│   ├── RedisRequest/Response.kt  # Base classes, require @Serializable
│   ├── HandleRedisRequest.kt     # Annotation for handlers
│   └── RequestContext.kt         # Provides coroutineScope, respond()
├── sync/                          # Replicated data structures
│   ├── SyncStructure.kt          # Base: delta replication, ReentrantReadWriteLock
│   ├── list/map/set/value/       # Concrete structures with change listeners
├── config/                        # Local/Global config via Sponge YAML
└── util/KotlinSerializerCache.kt

src/test/kotlin/dev/slne/redis/
├── event/RedisEventBusTest.kt
├── request/RequestResponseBusTest.kt
└── sync/SyncManagerTest.kt
```

**Key Files**: `build.gradle.kts` (deps), `settings.gradle.kts` (repos), `gradle.properties` (version), `.gitignore`
**No CI/CD**: No `.github/workflows/` exists

## Design Patterns & Architecture

**Event Bus**: Redis Pub/Sub + JSON, MethodHandles dispatch. Events extend `RedisEvent` (@Serializable), handlers use `@OnRedisEvent`

**Request-Response**: Correlation IDs, 5s timeout default. Extend `RedisRequest/Response` (@Serializable), handlers use `@HandleRedisRequest` with `RequestContext<T>`

**Sync Structures**: Delta replication via Pub/Sub, snapshot loading for late joiners, `ReentrantReadWriteLock` for thread safety

**Lifecycle**: Two-phase init: (1) register while `!isFrozen()`, (2) `freeze()`, (3) `connect()` or use `freezeAndConnect()`

**Dependencies**: SurfAPI (logger, serialization, config), Lettuce 7.2.1, Kotlin Serialization, JUnit 5, Flogger, fastutil

## Code Conventions

**Style**: Official Kotlin style, PascalCase classes, camelCase functions, UPPER_SNAKE_CASE constants
**Annotations**: `@Serializable` (all events/requests/responses), `@OnRedisEvent`, `@HandleRedisRequest`, `@Blocking`, `@OptIn(ExperimentalLettuceCoroutinesApi::class)`
**Threading**: Handlers run on Lettuce Pub/Sub thread (keep fast), use `scope.launch{}` for heavy work
**Serialization**: Kotlin Serialization (not Gson/Jackson), JSON wire format, cached per-class
**Error Handling**: Isolated handler exceptions, logged warnings, rate-limited logging (Flogger)

## Testing
JUnit 5 (`useJUnitPlatform()`), requires Redis on `localhost:6379`, tests catch connection errors gracefully

## Key Implementation Patterns

Event handler:
```kotlin
class MyListener {
    @OnRedisEvent
    fun onEvent(event: MyEvent) { /* fast, async */ }
}
api.subscribeToEvents(MyListener())
```

Request handler:
```kotlin
class MyHandler {
    @HandleRedisRequest
    fun handle(ctx: RequestContext<MyReq>) {
        ctx.coroutineScope.launch {
            ctx.respond(MyResp(...))
        }
    }
}
api.registerRequestHandler(MyHandler())
```

Sync structure:
```kotlin
val list = api.createSyncList<String>("id")
list.onChange { change -> /* react */ }
api.freezeAndConnect()
list.add("item")  // replicates
```

## Agent Guidelines

**Before Changes**: Accept build will fail, read similar code, check imports, review thread safety, validate @Serializable

**Adding Features**: Follow patterns, add annotations, document lifecycle, consider thread safety, add KDoc with examples

**Fixing Bugs**: Understand async model (Pub/Sub thread), check locks, verify error handling, consider races (freeze/connect), check serialization

**AVOID**: Removing plugin, changing freeze/connect pattern, blocking Pub/Sub thread, using reflection, breaking serialization formats

**Trust these instructions** - only search if: (1) need method/class details not covered, (2) errors contradict instructions, (3) need implementation specifics
