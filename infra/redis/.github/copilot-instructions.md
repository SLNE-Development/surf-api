# Copilot Instructions for surf-redis

## Project Overview
Kotlin library for Redis-based event distribution, request-response patterns, and synchronized data structures using **Redisson 4.1.0**, **Reactor**, and **Kotlin Coroutines**. Multi-module Gradle project: 55 Kotlin files (~6,100 LOC), 25 Lua scripts, ~2.5 MB.

**Stack**: Kotlin 1.9.22+, Java 25 (CI), Gradle 9.4.0, Redisson 4.1.0, Reactor, Kotlin Serialization, Testcontainers

## ⚠️ CRITICAL: Build Blocker

**Private Maven dependency `dev.slne.surf:surf-api-gradle-plugin:1.21.11+` at `https://repo.slne.dev/` is NOT accessible.**

All `./gradlew` commands FAIL with: `repo.slne.dev: No address associated with hostname`

**Java**: CI uses Java 25 (`export JAVA_HOME=/usr/lib/jvm/temurin-25-jdk-amd64`). Default is Java 17.

**Workarounds**:
- Focus on code review: syntax, imports, patterns, thread safety, serialization
- Use `view`/`grep`/`glob` tools to understand code
- Reference existing patterns for consistency
- DO NOT attempt to fix build or remove plugin

## Project Structure

**4 Subprojects**:
- **surf-redis-api/**: Public API (27 files) - RedisApi, event/request/sync/cache APIs, annotations
- **surf-redis-core/**: Implementation (21 files + 25 Lua scripts) - EventBusImpl, RequestResponseBusImpl, sync structures
- **surf-redis-paper/**: Paper/Folia plugin adapter (3 files)
- **surf-redis-velocity/**: Velocity proxy plugin adapter (2 files)

**Key Files**:
- `surf-redis-api/src/main/kotlin/dev/slne/surf/redis/RedisApi.kt`: Central entry point, lifecycle
- `surf-redis-api/api/surf-redis-api.api`: Binary API compatibility validation
- `surf-redis-core/src/main/resources/lua/`: Atomic Redis operations
- `.github/workflows/publish.yml`: CI (shadowJar, check, checkLegacyAbi)
- `README.md`: Comprehensive docs (470 lines) - READ lines 344-469 for pitfalls

## Build & Test (Requires Network Access)

CI workflow:
1. Setup: Java 25, Gradle 9.4.0
2. Build: `./gradlew shadowJar` (~45s)
3. Check: `./gradlew check checkLegacyAbi`
4. Publish: `./gradlew publish`

Tests use Testcontainers (requires Docker). Only 1 smoke test (commented out). `failOnNoDiscoveredTests = false`

Shadow JARs relocate deps under `dev.slne.surf.redis.libs.*` (netty, redisson, reactor, bytebuddy, etc.)

## Architecture & Patterns

**RedisApi Lifecycle** (two-phase):
1. Create: `RedisApi.create()`
2. Register listeners/handlers, create sync structures (while `!isFrozen()`)
3. `freeze()` - lock configuration
4. `connect()` - initialize Redisson, start components
5. `disconnect()` - shutdown

**Event Bus** (Redis Pub/Sub):
- Events: `@Serializable`, extend `RedisEvent`
- Handlers: `@OnRedisEvent`, MethodHandles dispatch
- **Synchronous** on Redisson/Reactor thread
- Delegate heavy work: `coroutineScope.launch {}`

**Request-Response** (broadcast, first-wins):
- `@Serializable` requests/responses extend `RedisRequest`/`RedisResponse`
- Handlers: `@HandleRedisRequest`, receive `RequestContext<T>`
- 5s timeout, respond once: `ctx.respond(response)`

**Sync Structures** (eventual consistency):
- `SyncList<E>`, `SyncSet<E>`, `SyncMap<K,V>`, `SyncValue<T>`
- Delta replication via Streams, change listeners
- **Must create before `freeze()`**

**Caches**: `SimpleRedisCache<K,V>`, `SimpleSetRedisCache<T>` with Lua atomic ops

**Deps**: Redisson 4.1.0, Kotlin Serialization (JSON, SnakeCase), Reactor, Testcontainers, SurfAPI plugin (proprietary)

## Code Conventions

**Style**: Kotlin official, PascalCase/camelCase/UPPER_SNAKE_CASE, KDoc for public APIs

**Required Annotations**:
- `@Serializable`: ALL events/requests/responses
- `@OnRedisEvent`, `@HandleRedisRequest`: Handler methods
- `@Blocking`: Blocking methods
- `@InternalRedisAPI`: Internal APIs (excluded from ABI checks)

**Threading**: Handlers synchronous on Reactor thread. Delegate work:
```kotlin
@HandleRedisRequest
fun handle(ctx: RequestContext<Req>) {
    ctx.coroutineScope.launch { ctx.respond(Resp(...)) }
}
```

**Serialization**: Kotlin Serialization only, JSON SnakeCase, UUID as string

**Common Pitfalls** (README 344-469):
1. Creating sync structures after `freeze()` → fails
2. Ignoring `originatesFromThisClient()` → self-handling loops
3. Blocking handlers → use `coroutineScope.launch {}`
4. Responding twice → only once per RequestContext
5. Assuming strong consistency → eventual consistency

**Error Handling**: Isolated exceptions, logged warnings, no auto-retry

## Examples

**Event**: `@Serializable class MyEvent : RedisEvent()`, handler `@OnRedisEvent fun on(e: MyEvent)`
**Request**: `@Serializable class MyReq : RedisRequest()`, handler `@HandleRedisRequest fun handle(ctx: RequestContext<MyReq>)`
**Sync**: `val list = api.createSyncList<String>("ns:id")`, then `api.freezeAndConnect()`, then `list.add("x")`

## Agent Guidelines

**Before Changes**:
1. Accept build will fail - focus on code quality
2. Find similar code via `grep`/`view`
3. Check annotations: `@Serializable`, `@OnRedisEvent`, etc.
4. Review thread safety and lifecycle constraints

**Adding Features**:
- Follow existing patterns (copy similar code)
- Add annotations, KDoc with examples
- Document lifecycle requirements
- Check README sections: 109-135 (Events), 144-172 (Request/Response), 190-260 (Sync Structures)

**Fixing Bugs**:
- Understand async model (Reactor thread)
- Check locks (sync structures use locks)
- Verify error handling isolation
- Consider freeze/connect race conditions

**AVOID**:
- ❌ Removing/replacing surf-api-gradle-plugin
- ❌ Changing freeze/connect pattern
- ❌ Blocking Reactor thread
- ❌ Reflection over MethodHandles
- ❌ Breaking JSON format
- ❌ Creating sync structures after `freeze()`
- ❌ Assuming strong consistency

**Validation** (no network):
1. Syntax check via `view`
2. Pattern match vs existing code
3. Validate imports, annotations
4. README consistency

**Trust these instructions** - only search for specific method signatures or when errors contradict these instructions.
