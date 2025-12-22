# Copilot Instructions for surf-redis

## Project Overview

**surf-redis** is a Kotlin library for Redis-based event distribution, request-response patterns, and synchronized data structures using Lettuce and Kotlin Coroutines. It provides asynchronous event publishing/subscribing across distributed systems, request-response patterns with timeout support, and replicated in-memory data structures (lists, maps, sets, values).

- **Repository Size**: ~700KB
- **Source Files**: 27 Kotlin files (23 main, 4 test)
- **Languages**: Kotlin 1.9.22+
- **Runtime**: JVM (Java 21 toolchain)
- **Dependencies**: Lettuce Redis client 7.2.1, Kotlin Serialization, custom SurfAPI framework
- **Build System**: Gradle 9.1.0 with Kotlin DSL
- **Project Type**: Library (produces artifacts for consumption by other projects)

## Critical Build Information

### ⚠️ Build Blockers

**CRITICAL**: The build system depends on a custom Gradle plugin `dev.slne.surf.surfapi.gradle.core:1.21.11+` that is **NOT publicly available**. The Maven repository `https://repo.slne.dev/repository/maven-public/` does not provide access to this plugin.

**Impact**: 
- Standard Gradle commands (`./gradlew build`, `./gradlew test`) **WILL FAIL** with plugin resolution errors
- Building from scratch is **NOT possible** without access to the private plugin
- The plugin provides: Kotlin plugin configuration, serialization setup, dependency management, and build configuration

### Available Java Versions
- Java 8, 11, 17, 21, 25 are installed in `/usr/lib/jvm/`
- **REQUIRED**: Java 21 (specified in `build.gradle.kts` via `jvmToolchain(21)`)
- Set Java 21: `export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64`
- Default Java is version 17, which is insufficient

### Build Commands (Will Fail Without Plugin Access)

```bash
# Set correct Java version FIRST
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64

# These commands will fail with plugin resolution error:
./gradlew clean         # Clean build artifacts
./gradlew build         # Compile and build (includes tests)
./gradlew build -x test # Build without running tests
./gradlew test          # Run tests only
./gradlew tasks         # List available tasks
```

**Expected Error**:
```
Plugin [id: 'dev.slne.surf.surfapi.gradle.core', version: '1.21.11+'] was not found
```

### Workaround Strategies

Since the build is blocked, when making code changes:
1. **Focus on code correctness** through code review and static analysis
2. **Validate imports** - ensure all required classes are available
3. **Check syntax** using IDE or Kotlin compiler directly if possible
4. **Review similar patterns** in existing code
5. **Document assumptions** about behavior that cannot be tested
6. **DO NOT** attempt to replace or remove the surf plugin without explicit approval

## Project Architecture

### Source Structure
```
src/
├── main/kotlin/de/slne/redis/
│   ├── RedisApi.kt              # Central Redis connection management
│   ├── event/                    # Event bus (pub/sub pattern)
│   │   ├── RedisEventBus.kt     # Main event bus implementation
│   │   ├── RedisEvent.kt        # Base class for events
│   │   └── OnRedisEvent.kt      # Annotation for event handlers
│   ├── request/                  # Request-response pattern
│   │   ├── RequestResponseBus.kt # Request/response bus
│   │   ├── RedisRequest.kt      # Base class for requests
│   │   ├── RedisResponse.kt     # Base class for responses
│   │   ├── RequestContext.kt    # Context for handlers
│   │   ├── HandleRedisRequest.kt # Annotation for request handlers
│   │   └── RequestTimeoutException.kt
│   ├── sync/                     # Synchronized data structures
│   │   ├── SyncStructure.kt     # Base class for sync structures
│   │   ├── list/SyncList.kt     # Replicated list
│   │   ├── map/SyncMap.kt       # Replicated map
│   │   ├── set/SyncSet.kt       # Replicated set
│   │   └── value/SyncValue.kt   # Replicated value
│   ├── config/                   # Configuration management
│   │   ├── InternalConfig.kt    # Internal config structure
│   │   ├── LocalConfig.kt       # Per-plugin config
│   │   └── GlobalConfig.kt      # Global Redis config
│   └── util/
│       └── KotlinSerializerCache.kt
└── test/kotlin/de/slne/redis/
    ├── event/RedisEventBusTest.kt
    ├── request/RequestResponseBusTest.kt
    └── sync/
        ├── SyncManagerTest.kt
        └── SyncStructuresTest.kt
```

### Key Design Patterns

1. **Event Bus Pattern**: Uses Redis Pub/Sub with JSON serialization for distributed events
   - Events extend `RedisEvent` and must be `@Serializable`
   - Handlers annotated with `@OnRedisEvent`
   - Uses MethodHandles and LambdaMetafactory for optimized dispatch

2. **Request-Response Pattern**: Async request/response with correlation IDs
   - Requests extend `RedisRequest`, responses extend `RedisResponse`
   - Handlers annotated with `@HandleRedisRequest`
   - Supports timeout configuration (default 5000ms)
   - Uses CompletableDeferred for async coordination

3. **Synchronized Structures**: Replicated in-memory data with delta replication
   - All structures extend `SyncStructure<TDelta>`
   - Uses Redis Pub/Sub for delta propagation
   - Thread-safe via ReentrantReadWriteLock
   - Supports late joiners via snapshot loading

4. **Lifecycle Management**: Two-phase initialization
   - Phase 1: Register handlers/listeners while `!isFrozen()`
   - Phase 2: Call `freeze()` to prevent further registration
   - Phase 3: Call `connect()` to establish Redis connections
   - Convenience: `freezeAndConnect()` combines phases 2 and 3

### Core Dependencies (from SurfAPI Plugin)

The code relies heavily on the custom SurfAPI framework:
- `dev.slne.surf.surfapi.core.api.serializer.SurfSerializerModule` - Serialization support
- `dev.slne.surf.surfapi.core.api.serializer.java.uuid.SerializableUUID` - UUID serialization
- `dev.slne.surf.surfapi.core.api.util.logger` - Flogger-based logging
- `dev.slne.surf.surfapi.core.api.config.*` - Configuration management (Sponge YAML)

External dependencies:
- `io.lettuce:lettuce-core:7.2.1.RELEASE` - Redis client
- Kotlin Coroutines - Async/await support
- Kotlin Serialization - JSON encoding/decoding
- JUnit 5 - Testing framework
- Google Flogger - Structured logging
- fastutil - High-performance collections
- Sponge Configurate - Configuration management

## Configuration Files

### build.gradle.kts
- Defines project dependencies and JVM toolchain
- Uses `tasks.test { useJUnitPlatform() }` for JUnit 5
- Specifies `kotlin { jvmToolchain(21) }`
- Group: `dev.slne`, Version: read from `gradle.properties`

### settings.gradle.kts
- Defines plugin repository: `maven("https://repo.slne.dev/repository/maven-public/")`
- Root project name: `surf-redis`

### gradle.properties
- `version=1.0.0-SNAPSHOT`
- `kotlin.code.style=official`
- `kotlin.stdlib.default.dependency=false`
- `org.gradle.parallel=true`

### .gitignore
- Ignores: `.gradle/`, `build/`, `out/`, `bin/`
- Ignores IDE files: `.idea/`, `*.iml`, `.classpath`, `.project`
- Standard ignore patterns for macOS and Windows

## Testing Strategy

### Test Framework
- **JUnit 5** (Jupiter) - specified via `useJUnitPlatform()`
- Uses `kotlin.test` assertions
- Tests use `runBlocking` for coroutine support

### Test Characteristics
- Most tests require a **running Redis instance** on `localhost:6379`
- Tests catch connection exceptions and allow them to pass (expected when Redis unavailable)
- Tests validate API structure and annotations without full integration

### Running Tests
```bash
export JAVA_HOME=/usr/lib/jvm/temurin-21-jdk-amd64
./gradlew test  # Will fail due to plugin issue
```

## Validation & CI/CD

### No CI/CD Configuration
- **No `.github/workflows/` directory exists**
- No GitHub Actions or other CI/CD pipelines defined
- No automated builds, tests, or deployments configured

### Manual Validation Steps
When the build system is functional:
1. Ensure Java 21 is active: `java -version` should show 21.x
2. Clean previous builds: `./gradlew clean`
3. Run full build: `./gradlew build`
4. Run tests separately: `./gradlew test`
5. Check for warnings in compilation output

## Code Style & Conventions

### Kotlin Style
- Follows `kotlin.code.style=official`
- Uses data classes for DTOs and configuration
- Companion objects for constants and factory methods
- Internal visibility for implementation details

### Naming Conventions
- Classes: PascalCase (e.g., `RedisEventBus`, `SyncStructure`)
- Functions: camelCase (e.g., `registerListener`, `freezeAndConnect`)
- Constants: UPPER_SNAKE_CASE (e.g., `DEFAULT_TIMEOUT_MS`, `REQUEST_CHANNEL`)
- Private fields: camelCase with no prefix

### Annotations
- `@Serializable` - Required on all events, requests, responses
- `@OnRedisEvent` - Marks event handler methods
- `@HandleRedisRequest` - Marks request handler methods
- `@Blocking` - Documents blocking methods
- `@OptIn(ExperimentalLettuceCoroutinesApi::class)` - For experimental Lettuce features

### Documentation
- KDoc for public APIs with parameter descriptions
- Includes usage examples in class-level documentation
- Documents thread safety and lifecycle requirements
- Notes blocking vs suspending behavior

## Common Patterns in Codebase

### Event Handler Registration
```kotlin
class MyListener {
    @OnRedisEvent
    fun onMyEvent(event: MyEvent) {
        // Handler code - invoked asynchronously
    }
}

val api = RedisApi.create(redisURI)
api.subscribeToEvents(MyListener())
api.freezeAndConnect()
```

### Request Handler Registration
```kotlin
class MyHandler {
    @HandleRedisRequest
    fun handleRequest(context: RequestContext<MyRequest>) {
        context.coroutineScope.launch {
            // Async work
            context.respond(MyResponse(...))
        }
    }
}

val api = RedisApi.create(redisURI)
api.registerRequestHandler(MyHandler())
api.freezeAndConnect()
```

### Synchronized Structure Usage
```kotlin
val api = RedisApi.create(redisURI)
val syncList = api.createSyncList<String>("my-list")
syncList.onChange { change ->
    // React to changes
}
api.freezeAndConnect()

// After connection
syncList.add("item")  // Replicates to all instances
```

## Important Implementation Notes

### Thread Safety
- Event handlers run on Lettuce Pub/Sub thread - keep them fast
- Sync structures use ReentrantReadWriteLock for local state
- Use `scope.launch { }` for heavy async work in handlers

### Serialization Requirements
- All events, requests, responses **MUST** be `@Serializable`
- Uses Kotlin Serialization (not Gson or Jackson)
- Serializers cached per-class for performance
- JSON format for wire protocol

### Error Handling
- Connection errors are logged but don't crash the application
- Serialization errors are caught and logged with warnings
- Handler exceptions are isolated (don't affect other handlers)
- Uses Google Flogger with structured logging and rate limiting

### Performance Optimizations
- MethodHandles + LambdaMetafactory for handler invocation (no reflection overhead)
- fastutil collections for reduced memory footprint
- Lettuce async APIs for non-blocking I/O
- Serializer caching to avoid repeated lookups

## Recommendations for Coding Agents

### Before Making Changes
1. **Accept that builds will fail** - the plugin dependency is unavailable
2. **Read similar code** to understand patterns and conventions
3. **Check imports** to ensure classes are available from dependencies
4. **Review thread safety** implications of changes
5. **Validate serialization** - ensure new classes are `@Serializable`

### When Adding Features
1. **Follow existing patterns** - event handlers, request handlers, sync structures
2. **Add proper annotations** - `@OnRedisEvent`, `@HandleRedisRequest`, `@Serializable`
3. **Document lifecycle** - explain when to call methods relative to freeze/connect
4. **Consider thread safety** - use appropriate locks and coroutine scopes
5. **Add KDoc** for public APIs with examples

### When Fixing Bugs
1. **Understand the async model** - handlers run on Pub/Sub thread
2. **Check lock usage** - ensure proper read/write lock usage
3. **Verify error handling** - exceptions should be caught and logged
4. **Consider race conditions** - especially around freeze/connect lifecycle
5. **Look for serialization issues** - ensure all types are serializable

### What to Avoid
- **DO NOT** remove or modify the surf plugin dependency
- **DO NOT** change the two-phase initialization (freeze/connect) pattern
- **DO NOT** block the Pub/Sub thread with heavy work
- **DO NOT** use reflection - use MethodHandles if needed
- **DO NOT** introduce breaking changes to serialization formats

### Testing Without Redis
- Tests gracefully handle connection failures
- Focus on API structure, annotations, and type safety
- Use `runBlocking` for coroutine tests
- Catch and allow `Connection refused` exceptions

## Trust These Instructions

These instructions are based on thorough examination of the codebase and validated command execution. **Trust this information** and only perform additional searches if:
- You need details about specific methods or classes not covered here
- You encounter errors that contradict these instructions
- You need to understand implementation details of a specific feature

The most important takeaway: **The build system is blocked by an unavailable Gradle plugin.** All code changes must be validated through code review and static analysis rather than compilation and testing.
