# surf-microservice

surf-microservice runs standalone JVM services that own a database and talk to the rest of the network over NATS.
A microservice is a Spring Boot application. The runtime hosts it in surf-api's root context, adds console commands
with a health check, and a Gradle plugin builds a runnable jar and a Dockerfile.

This guide is for developers who write or migrate a surf microservice. It assumes you know surf-api and surf-nats.
NATS contracts, services and events are documented in surf-nats.

## Modules

All artifacts use the group `dev.slne.surf.api`. The plugin adds the modules for you, so you rarely declare one
yourself. It requests them as the newest release of its own major version, for example `4.+`.

| Artifact | Package | Content |
|---|---|---|
| `surf-api-microservice-api` | `dev.slne.surf.microservice.api` | `MicroserviceInfo`, `MicroserviceCommand`, `MicroserviceCommands` |
| `surf-api-microservice-runtime` | `dev.slne.surf.microservice.runtime` | `runMicroservice`, lifecycle, command socket, console, spark, health check |
| `surf-api-microservice-database` | `dev.slne.surf.microservice.database` | Migrations by convention, `database` health indicator, NATS handler limits |
| `surf-api-microservice-reliability` | `dev.slne.surf.microservice.reliability` | `@NatsTransactional` handlers, `TransactionalEvents`, `ReliabilityConfig` |

The Gradle plugin `dev.slne.surf.api.gradle.microservice` is part of `surf-api-gradle-plugin`.

## Create a microservice

1. Apply the surf-api standalone plugin and the surf-nats plugin in the module's `build.gradle.kts`:

   ```kotlin
   plugins {
       id("dev.slne.surf.api.gradle.standalone") version "SURF_API_VERSION"
       id("dev.slne.surf.api.gradle.nats") version "SURF_API_VERSION"
   }
   ```

   Replace `SURF_API_VERSION` with the surf-api release you build against. Both plugins come from
   `surf-api-gradle-plugin` and share that version. The surf-nats plugin adds `surf-api-nats-spring` in exactly that
   version. The microservice plugin adds the microservice modules and `surf-api-database-r2dbc` as the newest
   release of the plugin's major version, for example `4.+`; `database.version` or the Gradle property
   `surf.database.version` pins the database (see [Configure the build](#configure-the-build)).

   On a standalone project the surf-nats plugin applies `dev.slne.surf.api.gradle.microservice` and adds
   `surf-api-nats-spring`. The microservice plugin then adds the API (`api`), the runtime (`implementation`) and,
   unless you disable them, the database and reliability modules. A service that does not use surf-nats applies
   `dev.slne.surf.api.gradle.microservice` itself instead of the surf-nats plugin. In every other case, do not apply it
   yourself.

2. Declare a Spring Boot application class and a `main` function in the module's package, outside
   `dev.slne.surf.api`:

   ```kotlin
   import dev.slne.surf.microservice.runtime.runMicroservice
   import org.springframework.boot.autoconfigure.SpringBootApplication

   @SpringBootApplication
   class PunishApplication

   fun main(args: Array<String>) {
       runMicroservice<PunishApplication>(args)
   }
   ```

   `runMicroservice` returns `Nothing`, so `main` needs a block body. An expression body does not compile. The plugin
   finds the `main` function, see [Main class](#main-class).

3. Run it locally:

   ```shell
   ./gradlew :MODULE:runMicroservice
   ```

   Replace `MODULE` with the Gradle path of the microservice module.

## Configure the build

The `surfMicroservice` extension has an optional `mainClass` and three blocks. Every value has a default, so a
service only declares what it changes:

```kotlin
surfMicroservice {
    database {
        enabled = true
        version = "4.0.0"
    }
    reliability {
        enabled = false
    }
    docker {
        memoryBudget = "2g"
        jvmOptions.add("-XX:ActiveProcessorCount=4")
    }
}
```

Database and reliability values are read when the dependencies are first resolved, so you can set them anywhere in
the build script. The following Gradle properties provide the defaults when the DSL leaves a value unset:

| Gradle property | DSL value | Default |
|---|---|---|
| `surf.microservice.database.enabled` | `database.enabled` | `true` |
| `surf.database.version` | `database.version` | The major version of the plugin followed by `.+`, for example `4.+` |
| `surf.microservice.reliability.enabled` | `reliability.enabled` | Enabled when the database is enabled and surf-nats is present |

The boolean properties accept only `true` or `false`. Any other value fails the build.

`database.enabled` adds `surf-api-microservice-database` and `dev.slne.surf.api:surf-api-database-r2dbc` in
`database.version` to `implementation`.

`reliability.enabled` adds `surf-api-microservice-reliability` to `implementation` and its test fixtures to
`testImplementation`. surf-nats counts as present when the `dev.slne.surf.api.gradle.nats` plugin is applied or
`surf-api-nats-spring` is declared in `api`, `implementation` or `runtimeOnly`. Enabling reliability explicitly while
the database is disabled or surf-nats is missing fails the build when the dependencies resolve.

For the `docker` block, see [Generate the Dockerfile](#generate-the-dockerfile).

## Main class

A microservice declares a `@SpringBootApplication` class and a top-level `main` function next to it:

```kotlin
@SpringBootApplication
class PunishApplication

fun main(args: Array<String>) {
    runMicroservice<PunishApplication>(args)
}
```

The plugin sets the `Main-Class` of the shadow jar from the compiled classes. `detectMicroserviceMainClass` takes the
single class with a `public static void main(String[])`, which is the `PunishApplicationKt`-style file class of the
Kotlin `main`. It skips synthetic classes, such as the part classes of a multifile facade. It fails the build when the
module has no such class or several. Set the class yourself to choose between several, or to name a class the
detection cannot find:

```kotlin
surfMicroservice {
    mainClass = "dev.slne.punish.PunishApplicationKt"
}
```

With the `application` plugin, `application.mainClass` serves as the value when `surfMicroservice.mainClass` is not
set. Setting `shadowJar.mainClass` yourself takes precedence over both and skips the detection.

`runMicroservice` and the Docker image run the jar with `java -jar`, so neither needs the class name.

## Fat jar registries

Spring Boot and the surf modules each ship parts of the same registries. The shadow jar merges them instead of
keeping one copy:

- `META-INF/services/*` and `META-INF/spring/*.imports`: line by line, every entry of every jar once.
- `META-INF/spring.factories` and `META-INF/spring/aot.factories`: per key, the values of all jars joined with `,`.
  Boot and the surf modules both define keys such as `org.springframework.context.ApplicationContextInitializer`.
- `META-INF/spring.handlers` and `META-INF/spring.schemas`: per key, the first value.
- The log4j plugin cache `Log4j2Plugins.dat`: the caches of log4j-core and of every other jar that ships log4j
  plugins, combined into one, so log4j finds its own appenders and layouts.

## Name and data directory

`MicroserviceInfo` is a bean that identifies the service in logs, as the database pool name, as the NATS client name
and in the migration history table. Inject it where you need the name or the data directory. The name comes from the
first of these that is set:

1. The environment variable `SURF_MICROSERVICE_NAME`.
2. The property `spring.application.name`.
3. The class name of the application without an `Application` or `Microservice` suffix, in kebab case:
   `PunishApplication` and `PunishMicroservice` both become `punish`.

The start fails unless the name consists of lowercase letters and digits separated by single hyphens, with at most
40 characters.

`MicroserviceInfo.dataPath` is the directory for configuration files. It comes from the environment variable
`SURF_MICROSERVICE_CONFIG_DIR`, otherwise it is `config` in the working directory. The runtime publishes the name as
`spring.application.name` and the directory as the property `surf.config-directory`, ahead of every other property
source, so surf modules such as the database and surf-nats read the same values. The environment variable
`SURF_CONFIG_DIRECTORY`, which sets that property outside a microservice, has no effect here: set
`SURF_MICROSERVICE_CONFIG_DIR` instead.

| File | Content |
|---|---|
| `<dataPath>/database.yml` | Database connection and pool, overridable with the `SURF_DATABASE_*` variables of surf-database |
| `<dataPath>/nats/config.yml` | NATS connection and handler limits, overridable with the `SURF_NATS_*` variables of surf-nats |
| `<dataPath>/spark/` | spark profiler data |

## Use the database

The database auto-configuration opens the connection pool and migrates the database while the context starts. It
closes the pool when the context closes. Inject the Exposed `R2dbcDatabase` where you need it. Every bean that injects
it sees the migrated schema:

```kotlin
@Repository
class BanRepository(private val database: R2dbcDatabase) {

    suspend fun count(player: UUID): Long = suspendTransaction(database) {
        BansTable.selectAll().where { BansTable.player eq player }.count()
    }
}
```

The connection settings, the `SURF_DATABASE_*` overrides and the `DatabaseConfig` bean are described in the
[surf-database-r2dbc README](../database-r2dbc/README.md#configuration). Inject `SurfMigrationRunner` to run
additional migrations on demand.

### Migrations

The auto-configuration registers a `MigrationSet` for the service itself. It finds the migrations by convention:

- SQL files in `src/main/resources/db/migration`, such as `V1__create_bans.sql`.
- Every `SurfMigration` below the package of the `@SpringBootApplication` class that is an `object`, a companion
  object or a class with a public constructor without parameters. Abstract classes, and non-final classes whose
  constructors all take parameters, are skipped, so a shared base class may have constructor parameters; its
  subclasses run.

The history of the set lives in `flyway_schema_history_<name>`, with every `-` in the service name replaced by `_`.
The set runs before the sets of libraries, which use the lowest precedence.

If a service needs different SQL per database vendor, put the files in `db/migration/mariadb` and
`db/migration/postgresql`. Once one of these folders exists, only the folder of the configured vendor runs and SQL
files directly in `db/migration` are ignored. The start fails if vendor folders exist but none for the configured
vendor. Both folders share one history table, so keep their versions aligned.

Without migrations found by convention, the service gets no set and no history table. To skip the set although the
service ships migrations, set the property `surf.database.migrations.enabled` to `false`, for example in
`application.properties`. A service that creates its schema with `SchemaUtils` needs no such property, because it
ships no migrations.

### Add migrations and customize the database

Further migrations are `MigrationSet` beans. Each set has its own name and history table and runs in `@Order` order
before the `R2dbcDatabase` bean exists:

```kotlin
@Component
class ExtraMigrations : MigrationSet {

    override val name = "punish_extra"

    override fun MigrationSpec.describe() {
        sql("db/extra/${MigrationSpec.VENDOR_PLACEHOLDER}")
    }
}
```

A `SurfDatabaseConfigurer` bean adjusts the Exposed configuration after the defaults have been applied:

```kotlin
@Bean
fun statementLimits() = SurfDatabaseConfigurer { it.defaultFetchSize = 500 }
```

The property names are those of Exposed's `R2dbcDatabaseConfig.Builder`.

The database auto-configuration sets Exposed's `defaultMaxAttempts` to `1`, where Exposed's own default is `3`. A
`suspendTransaction` runs its block once and never repeats on its own, so a block with side effects outside the
database does not run twice unnoticed. To retry a transient conflict, such as a deadlock, call
`database.retryingTransaction { ... }`. Configurers run after this default, so a configurer can restore Exposed's
retries for every transaction:

```kotlin
@Bean
fun exposedRetries() = SurfDatabaseConfigurer { it.defaultMaxAttempts = 3 }
```

A `DatabaseConfig` bean of the application replaces the file `database.yml`. The start validates it and then uses it
as it is, without the `SURF_DATABASE_*` overrides.

### NATS handler limits

surf-nats limits how many handlers of one service registration run at the same time. When surf-nats is on the class
path, the database auto-configuration sets these defaults from the maximum pool size before surf-nats registers any
service:

- Concurrent handlers: twice the pool size.
- Concurrent long-running handlers: half the pool size, at least one.

With the default pool of 10 connections, a registration runs at most 20 handlers, 5 of them long-running. The limits
only change surf-nats defaults: `max-concurrent-handlers`, `max-concurrent-long-handlers` and their `-by-service`
entries in `nats/config.yml`, and the variables `SURF_NATS_MAX_CONCURRENT_HANDLERS` and
`SURF_NATS_MAX_CONCURRENT_LONG_HANDLERS`, take precedence.

## Lifecycle

`runMicroservice` selects log4j, builds the Spring Boot application without web server and banner, and runs it. The
context is surf-api's root context. A start runs in this order:

1. Resolve `MicroserviceInfo` and publish the name and data directory as properties.
2. Refresh the context. Auto-configurations create their beans: the database opens and migrates, surf-nats declares its
   client and service proxies. Once all singletons exist, the built-in commands and every `MicroserviceCommand` bean are
   registered.
3. Start the lifecycle beans in ascending phase:

   | Phase | Component |
   |---|---|
   | `-1000` | spark |
   | `0` | The surf-api hooks: `onLoad`, then `onEnable` of every `SurfLifecycle` bean. Beans with `@SurfEventHandler` methods are registered on the surf event bus before `onEnable` and unregistered after `onDisable`. surf-nats connects before the first `onLoad` and registers `@NatsService` and `@NatsListener` beans when the hooks enable the application. |
   | `1000` | The command socket and the console |

A bean that needs to start or stop at another point implements Spring's `SmartLifecycle` and chooses a phase. A phase
below `0` starts before the surf-api hooks and stops after them. A phase above `0` starts after them and stops before
them. Put one-time setup that needs the whole application, such as warming a cache, into `onEnable` of a
`SurfLifecycle` bean, and the matching cleanup into `onDisable`:

```kotlin
@Component
class PunishLifecycle(private val cache: BanCache) : SurfLifecycle {

    override suspend fun onEnable() {
        cache.warmUp()
    }

    override suspend fun onDisable() {
        cache.flush()
    }
}
```

The command socket and the console start after the hooks and stop before them, so a command only runs while the
application is enabled.

The service stops after the `exit` command, `SIGTERM`, or the close of the context, in reverse order:

1. Close the command socket and the console.
2. Drain NATS: surf-nats stops accepting incoming events and requests and waits for running handlers up to the NATS
   drain timeout. Then run `onDisable`. Publishing, service calls and database access still work in `onDisable`.
3. Stop spark.
4. Close the beans, dependents before their dependencies. The database pool closes after every bean that injects
   the database.
5. Shut log4j down. Spring Boot's logging system is off, because log4j shuts down last.

If the start fails, Spring Boot stops what already started and closes the context. A lifecycle bean that did not start
is not stopped.

A lifecycle bean starts at most once. Calling `ApplicationContext.start()` after `stop()` fails with an
`IllegalStateException`, because the runtime components cannot restart.

### Exit codes

| Cause | Exit code |
|---|---|
| Failed start | `1` |
| `exit` command | `0` |
| JVM shutdown signal, such as `SIGTERM` | The exit code of the JVM for that signal |
| `SpringApplication.exit` or `close()` called by a bean | The code that `SpringApplication.exit` computed, otherwise `0` |

After a failed start or the `exit` command, the process ends even if a library left non-daemon threads behind.

## Commands

Every microservice accepts commands on its standard input and on a Unix domain socket that the `microservicectl`
client in the Docker image uses. The built-in commands are:

| Command | Effect |
|---|---|
| `help` | Lists all commands with their aliases and descriptions. |
| `health` | Runs all health checks. Fails with one line per unhealthy component. |
| `spark ARGS` | Runs a spark command, such as `spark health` or `spark profiler start`. |
| `exit`, `quit` | Shuts the microservice down. |

Labels match case-insensitively. On the console, arguments are split at whitespace. Single quotes keep their content
literally, double quotes group words, and a backslash outside single quotes takes the next character literally:

```text
mute "Player Name" 'spam in chat'
```

`microservicectl` passes its arguments as given, so the shell does the quoting there:

```shell
microservicectl mute "Player Name" "spam in chat"
```

`microservicectl` exits with `0` on success, `1` when the command fails, `2` for an invalid request and `3` when it
cannot reach the service.

### Register a command

Extend `MicroserviceCommand` and declare the class as a bean. The auto-configuration registers every
`MicroserviceCommand` bean in `@Order` order once all singletons exist:

```kotlin
@Component
class BansCommand(private val bans: BanRepository) : MicroserviceCommand("bans", "b") {
    override val description = "Counts the active bans: bans <player uuid>"

    override suspend fun MicroserviceCommandContext.execute(args: List<String>) {
        val player = args.firstOrNull()?.let { runCatching { UUID.fromString(it) }.getOrNull() }
            ?: fail("Usage: bans <player uuid>")
        sendLine("Active bans of $player: ${bans.count(player)}")
    }
}
```

To register a command at any other time, call `MicroserviceCommands.register(command)`, or create and register one in
a single call with `microserviceCommand`. Both use one registry per JVM:

```kotlin
microserviceCommand("echo", "say", description = "Repeats its arguments.") { args ->
    if (args.isEmpty()) fail("Usage: echo <text>")
    sendLine(args.joinToString(" "))
}
```

Registering a name or alias that is already taken throws `IllegalArgumentException`, which fails the start for a bean.
The runtime registers the built-in commands first. A built-in is skipped with a warning if one of its labels is taken
already, so a command registered earlier, for example in `main`, replaces it. A replacement of `exit` has to call
`requestShutdown()` itself. A `MicroserviceCommand` bean that reuses the label of a built-in fails the start.

Inside a command, `sendLine` and `sendError` write to the caller, `fail(message)` ends the command as failed without
a stack trace, and `requestShutdown()` shuts the service down after the response. Any other exception is logged
with its stack trace and reported as a failure.

### Contribute a health check

Declare a Spring Boot `HealthIndicator` as a bean. The `health` command, and with it the Docker `HEALTHCHECK`, runs
every indicator that the `HealthContributorRegistry` holds:

```kotlin
@Component
class CacheHealthIndicator(private val cache: Cache) : HealthIndicator {

    override fun health(): Health =
        if (cache.ping()) Health.up().build() else Health.down().withDetail("error", "The cache is unreachable").build()
}
```

The component name is the bean name without the `HealthIndicator` suffix: `cacheHealthIndicator` reports `cache`. A
composite contributor reports each of its indicators as `name[instance]`. An unhealthy result shows the detail `error`
as its message, otherwise the status description. The runtime runs all indicators concurrently and reports an
indicator that throws or takes longer than 3 seconds as unhealthy. Reactive contributors are not checked.

The `database` indicator (`SELECT 1` within 2.5 seconds) and the `nats` indicator of surf-nats are contributed
automatically. Spring Boot's own indicators, such as disk space, are off. Set a property such as
`management.health.defaults.enabled=true` to turn them on.

### Command socket

The runtime listens on `SURF_MICROSERVICE_COMMAND_SOCKET`, default `/tmp/surf-microservice.sock`. `microservicectl`
reads the same variable. A Unix domain socket path may have at most 100 bytes. For a longer path the runtime listens
at `<java.io.tmpdir>/surf-microservice-<hash>.sock` instead and logs both paths. `microservicectl` finds that path
only if the variable points to it, so set a short path.

## Run locally

`runMicroservice` builds the Shadow jar and runs it with `java -jar` on the project's Java toolchain. The working
directory is `run/` in the project directory, so configuration and logs survive `clean`. The task forwards standard
input, so you can type commands into the Gradle console.

## Generate the Dockerfile

To write a Dockerfile for the service, run:

```shell
./gradlew :MODULE:generateMicroserviceDockerfile
```

The task writes `Dockerfile` into the module directory. The image builds from the root project directory as context
in three stages: Gradle builds the Shadow jar, a Go stage compiles `microservicectl`, and the runtime stage runs the
jar as the unprivileged user `app` in `/app`. The generated file contains:

- `HEALTHCHECK` that runs `microservicectl health` every 30 seconds with a 10-second timeout, a 60-second start
  period and 3 retries.
- `STOPSIGNAL SIGTERM`. The entrypoint `exec`s java, so the signal reaches Spring Boot's shutdown hook.
- `SURF_MICROSERVICE_COMMAND_SOCKET=/app/run/microservice.sock`.
- An entrypoint script that starts java with the JVM settings described in [JVM settings](#jvm-settings). Arguments
  passed to the container are forwarded to the service.

Configure the output in the `docker` block:

```kotlin
surfMicroservice {
    docker {
        outputFile = layout.projectDirectory.file("deploy/Dockerfile")
        baseImage = "eclipse-temurin:25-jre"
        builderImage = "eclipse-temurin:25-jdk"
        overwrite = false
    }
}
```

| Value | Default |
|---|---|
| `outputFile` | `Dockerfile` in the project directory |
| `baseImage` | `eclipse-temurin:<toolchain version>-jre`, Java 25 without a toolchain |
| `builderImage` | `eclipse-temurin:<toolchain version>-jdk`, Java 25 without a toolchain |
| `commandClientBuilderImage` | `golang:1.26-alpine` |
| `overwrite` | `false` |
| `jvmOptions` | See [JVM settings](#jvm-settings) |
| `memoryBudget` | `1g` |

With `overwrite = false` the task replaces only a file that carries the generated marker, so a hand-written
Dockerfile stays untouched. Set `overwrite = true` to replace it.

The Dockerfile sets only the JVM settings and the command socket. Pass service configuration, such as database and
NATS credentials, as runtime environment variables or secrets.

### JVM settings

The container starts java with these options, in this order. For options with a value, such as
`-XX:ActiveProcessorCount=4`, the last one wins. Collector flags add up instead: to use another collector, add
`-XX:-UseG1GC` as well or replace `docker.jvmOptions`.

1. `docker.jvmOptions`. The defaults target HotSpot 25: `-XX:+UseG1GC -XX:+UseCompactObjectHeaders
   -XX:MaxRAMPercentage=60.0 -XX:ActiveProcessorCount=2 -XX:G1PeriodicGCInterval=60000
   -XX:TrimNativeHeapInterval=60000 -XX:+ExitOnOutOfMemoryError`. `jvmOptions.add(...)` appends to them. Assigning
   a list replaces them, for example for another JVM or an older Java version, which reject unknown options at
   startup. Each option must start with `-` and contain no whitespace or shell metacharacters.
2. `-XX:MaxRAM=<budget>`, but only when cgroup v2 shows no memory limit for the container or any of its parent
   cgroups. The budget comes from `SURF_MICROSERVICE_MEMORY_BUDGET`, default `docker.memoryBudget`. An empty value
   turns it off. On cgroup v1 hosts the budget is not applied, and an unlimited container sizes the JVM from the host
   memory.
3. `JAVA_OPTS`.

The image also sets `MALLOC_ARENA_MAX=2`, which limits the native memory glibc keeps per malloc arena.

Without a memory limit, the JVM sizes its heap from the host memory and keeps memory it once used. The defaults let
an idle service return memory after a load peak. A service that needs more memory sets
`SURF_MICROSERVICE_MEMORY_BUDGET=2g` or a container memory limit. One that needs more parallelism sets
`JAVA_OPTS=-XX:ActiveProcessorCount=4`.

```kotlin
surfMicroservice {
    docker {
        memoryBudget = "2g"
        jvmOptions = listOf("-XX:+UseSerialGC", "-XX:MaxRAMPercentage=60.0", "-XX:+ExitOnOutOfMemoryError")
    }
}
```

### spark

spark's commands, such as `microservicectl spark health`, are always available. The sampling profiler keeps its call
tree on the heap, which raises the idle memory and CPU of a service, so it does not start with the service. Start it
on demand with `microservicectl spark profiler start`, or with the service by setting
`SURF_MICROSERVICE_SPARK_PROFILER=true`. Any value other than `true` or `false` stops the service start.

## Environment variables

The runtime and the generated image read the following variables:

| Variable | Default | Effect |
|---|---|---|
| `SURF_MICROSERVICE_NAME` | `spring.application.name`, otherwise the class name in kebab case | Service name, see [Name and data directory](#name-and-data-directory) |
| `SURF_MICROSERVICE_CONFIG_DIR` | `config` | Data path for configuration files, published as `surf.config-directory` |
| `SURF_MICROSERVICE_COMMAND_SOCKET` | `/tmp/surf-microservice.sock`, `/app/run/microservice.sock` in the image | Command socket of the runtime and `microservicectl` |
| `SURF_MICROSERVICE_SPARK_PROFILER` | `false` | Starts the spark profiler with the service when `true` |
| `SURF_MICROSERVICE_MEMORY_BUDGET` | `docker.memoryBudget` | `-XX:MaxRAM` for containers without a memory limit, image only |
| `JAVA_OPTS` | Empty | JVM options appended last, image only |
| `MALLOC_ARENA_MAX` | `2` in the image | glibc malloc arena limit |

surf-database reads `SURF_DATABASE_*` and surf-nats reads `SURF_NATS_*` in addition.

## Logging

The runtime ships a `log4j2.xml` that logs to the console and to `log/latest.log`, with daily and size-based
rollover. It sets `shutdownHook="disable"`, because the runtime shuts log4j down as its last step. If a service ships
its own `log4j2.xml`, set the attribute there too:

```xml
<Configuration status="WARN" shutdownHook="disable">
```

Without it, log4j registers its own shutdown hook and can stop logging while the service is still shutting down.

## Idempotent handlers and events after the commit

The module `surf-api-microservice-reliability` makes persistent NATS handlers run at most once per message and
publishes their events after the commit. The plugin adds it by default, see
[Configure the build](#configure-the-build).

The module contributes these beans:

- A `ReliabilityMigrationSet` that migrates its own table (`surf_atomic_operations`, history table
  `flyway_schema_history_surf_reliability`) after the service's own migrations, also when
  `surf.database.migrations.enabled` is `false`. On MariaDB the start fails unless the server is 10.6 or newer and the
  tables use InnoDB.
- `AtomicOperations` on the microservice database, the interceptor that makes `@NatsTransactional` services
  idempotent, `TransactionalEvents`, and a cleanup that deletes expired stored replies.
- `ReliabilityConfig` with the defaults, see below.

Mark the `@NatsService` bean `@NatsTransactional`, and publish events from its handlers with the injected
`TransactionalEvents`:

```kotlin
@NatsService
@NatsTransactional
class PunishmentServiceImpl(
    private val database: R2dbcDatabase,
    private val events: TransactionalEvents,
) : PunishmentService {
    override suspend fun createBan(request: CreateBanRequest): CreateBanResponse {
        val ban = suspendTransaction(db = database) { insertBan(request) }
        events.publish(BanCreated(ban.id, ban.revision), aggregateId = ban.id)
        return CreateBanResponse(ban.id)
    }
}
```

The start fails if a `@NatsTransactional` bean has no reliability module behind it, so the service never runs without
idempotency by accident. The stored replies belong to the microservice name.

Every persistent method of such a service runs in one READ COMMITTED transaction on the microservice database, which
`suspendTransaction` on that database joins. The handler's statements and its encoded reply commit together, keyed by
the proto method and the NATS message id. A redelivery, the same message on another instance, or a dead-letter replay
of a committed message receives the stored reply without running the handler again. Methods that are not persistent
are not affected.

| Situation | Result |
|---|---|
| Handler throws | Rollback, nothing stored. surf-nats answers the RPC with the error or redelivers the fire call. |
| Database unavailable, lock wait over 10 s, commit fails | No reply. The message is redelivered (`NatsRedeliveryException`). |
| Deadlock after the handler ran | Rollback. The message is redelivered instead of retried in process. |
| Same message id, different request | Error reply `AtomicOperationConflictException`. |

Events published through `TransactionalEvents` are held in memory. Once the handler's transaction has committed, they
are published in publish order with `NatsClient.publish` on the client the service is registered on, so they carry its
`Surf-Origin`: listeners of that client see them as their own (`isFromThisClient`), and
`@NatsListener(ignoreFromThisClient = true)` skips them. A handler that throws or rolls back publishes nothing, and
neither does a redelivery answered with the stored reply. Outside a transactional handler `publish` throws. Use
`NatsClient.publish` there.

Delivery is best effort, as for every core NATS event. If the instance stops between the commit and the publish, or a
publish fails because NATS is unavailable, the event is lost and the failure is logged. There is no retry and nothing
is stored in the database. Listeners that are offline miss events. The event is not validated before the commit, so
an event that is no generated event message or exceeds the payload limit is logged as lost.

Settings come from a `ReliabilityConfig` bean. An application bean replaces the default:

```kotlin
@Bean
fun reliabilityConfig() = ReliabilityConfig(
    atomicOperations = AtomicOperationsConfig(retention = 7.days),
)
```

Stored replies are kept for `NatsPersistentLimits.REDELIVERY_HORIZON` (14 days) by default, the longest time after
which a message with the same id can reach a handler. A shorter retention lets a late redelivery or dead-letter
replay run the handler again. Expired replies are deleted every hour (`cleanupInterval`).

An application `AtomicOperations` bean replaces the default one, and then both the interceptor of `@NatsTransactional`
services and the cleanup use it. Build it on the microservice's own `R2dbcDatabase` and keep its `retention` at
`NatsPersistentLimits.REDELIVERY_HORIZON` or longer:

```kotlin
@Bean
fun atomicOperations(database: R2dbcDatabase) = AtomicOperations(
    database,
    AtomicOperationsConfig(retention = NatsPersistentLimits.REDELIVERY_HORIZON),
)
```

A bean on another database stores the replies where the service's own transactions do not commit, and a shorter
retention lets a late redelivery run the handler again. Either way the guarantee that a handler runs at most once per
message no longer holds. Prefer `ReliabilityConfig` unless you need a custom `AtomicOperations`.

### Test transactional services

`TransactionalHandlerFixture` from the test fixtures runs code as a transactional handler against a test database, so
a service receives the same `TransactionalEvents` as in production and needs no wrapper around it. NATS need not run,
and nothing is published.

The fixture needs the test database and its `SurfMigrationRunner`, for example from a test context that runs
`SurfDatabaseAutoConfiguration` as described under [Testing](../database-r2dbc/README.md#testing) in the
surf-database-r2dbc README:

```kotlin
val fixture = TransactionalHandlerFixture.create(database, migrationRunner)
val service = PunishmentServiceImpl(database, fixture.events)

fixture.runInHandler { service.createBan(request) }

assertEquals(1, fixture.publishedOf<BanCreated>().size)
assertEquals(listOf(banId), fixture.published.map { it.aggregateId })
```

`create` migrates the reliability schema. Each `runInHandler` call runs its block in one READ COMMITTED transaction,
which `suspendTransaction` on that database joins. `published` lists the events the committed blocks would publish,
in publish order. A block that throws rolls back and adds nothing. `clear()` forgets the recorded events. Calling
`events.publish` outside `runInHandler` throws, as it does outside a handler in production.

## Upgrade from v3

4.0 turns a microservice into a Spring Boot application and removes the `Microservice` base class. There is no
compatibility layer. [MIGRATION-4.0.md](../../MIGRATION-4.0.md#14-microservices-became-spring-boot-applications)
maps every removed type to its replacement. To move a v3 microservice:

1. In `build.gradle.kts`, apply `dev.slne.surf.api.gradle.standalone` and `dev.slne.surf.api.gradle.nats`. Remove
   `dev.slne.surf.microservice`, `dev.slne.surf.nats` and the KSP processor, and replace the old coordinates with the
   group `dev.slne.surf.api`. The Kotlin packages of the microservice modules stay the same.
2. Replace the `Microservice` subclass with a `@SpringBootApplication` class and a `main` function with a block body
   that calls `runMicroservice`. Remove `@AutoService(Microservice::class)`.
3. Delete the `name` and `dataPath` overrides. Set `spring.application.name` or `SURF_MICROSERVICE_NAME` if the default
   name does not fit.
4. Move the body of `onBootstrap` and `onDisable` into a `SurfLifecycle` bean, or into the constructors and
   `@Bean` methods of the beans that own the resources.
5. Replace `DatabaseApi` and `microservice.database` with the injected `R2dbcDatabase`. Replace `DatabaseCustomizer`
   with `SurfDatabaseConfigurer` and `MigrationSet` beans. Set `surf.database.migrations.enabled` to `false` where
   `runMigrations` was `false`.
6. Declare each command as a `MicroserviceCommand` bean and each health check as a `HealthIndicator` bean. Delete the
   `MicroserviceIntegration` classes and open their resources in beans.
7. Remove your own `NatsClient.create(...)` call. The context provides the client, named after the microservice. To
   keep the old client name during a rolling deploy, see
   [Microservice client name](../nats/README.md#microservice-client-name).
8. Replace `registerTransactionalService` with `@NatsTransactional` on the `@NatsService` bean, `microservice.events`
   with the injected `TransactionalEvents`, and `ReliabilityCustomizer` with a `ReliabilityConfig` bean.
9. Regenerate the Dockerfile with `generateMicroserviceDockerfile` and set `overwrite = true` once if the old file
   lacks the generated marker.

To verify the upgrade, run `./gradlew :MODULE:runMicroservice` and type `health`. Once the database and NATS are
reachable, the command prints a line that starts with `healthy`.
