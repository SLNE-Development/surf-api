# surf-microservice

surf-microservice runs standalone JVM services that own a database and talk to the rest of the network over NATS.
It provides the `Microservice` base class, a runtime that starts and stops the service, its database and NATS in a
fixed order, console commands with a health check, and a Gradle plugin that builds a runnable jar and a Dockerfile.

This guide is for developers who write or migrate a surf microservice. It assumes you know surf-api and surf-nats.
NATS contracts, services and events are documented in surf-nats.

## Modules

All artifacts use the group `dev.slne.surf.microservice`. The plugin adds them for you, so you rarely declare one
yourself.

| Artifact | Package | Content |
|---|---|---|
| `surf-microservice-api` | `dev.slne.surf.microservice.api` | `Microservice`, commands, health contributors, integration SPI |
| `surf-microservice-runtime` | `dev.slne.surf.microservice.runtime` | Launcher, lifecycle, command socket, console, spark |
| `surf-microservice-database` | `dev.slne.surf.microservice.database` | Database integration, `Microservice.database`, `DatabaseCustomizer` |
| `surf-microservice-reliability` | `dev.slne.surf.microservice.reliability` | Transactional services, `Microservice.events` |
| `surf-microservice-processor` | `dev.slne.surf.microservice.processor` | KSP processor that finds the main class and the migrations |
| `surf-microservice-gradle-plugin` | `dev.slne.surf.microservice.gradle` | Gradle plugin `dev.slne.surf.microservice` |

## Create a microservice

1. Apply the surf-api standalone plugin and the surf-nats plugin in the module's `build.gradle.kts`:

   ```kotlin
   plugins {
       id("dev.slne.surf.api.gradle.standalone")
       id("dev.slne.surf.nats")
   }
   ```

   On a standalone project the surf-nats plugin applies `dev.slne.surf.microservice` and adds `surf-nats-standalone`
   in its own version. The microservice plugin then adds the API (`api`), the runtime (`runtimeOnly`), the KSP
   processor (`ksp`) and, unless you disable them, the database and reliability modules. Do not apply
   `dev.slne.surf.microservice` yourself.

2. Declare exactly one concrete subclass of `Microservice` in the module, either a class with a public no-argument
   constructor or an `object`:

   ```kotlin
   import dev.slne.surf.microservice.api.Microservice
   import dev.slne.surf.microservice.database.database
   import dev.slne.surf.nats.NatsClient
   import dev.slne.surf.nats.registerService

   class PunishMicroservice : Microservice() {
       private lateinit var client: NatsClient

       override suspend fun onBootstrap(args: List<String>) {
           client = NatsClient.create("punish")
           client.registerService<PunishmentService>(PunishmentServiceImpl(database))
       }

       override suspend fun onDisable() {
           client.close()
       }
   }
   ```

   `PunishmentService` stands for a service interface generated from your `.proto` contract.

3. Run it locally:

   ```shell
   ./gradlew :MODULE:runMicroservice
   ```

   Replace `MODULE` with the Gradle path of the microservice module.

The runtime creates the instance before surf-api, the database and NATS start. Keep constructors and property
initializers free of side effects and acquire resources in `onBootstrap`.

## Configure the build

The `surfMicroservice` extension has three blocks. Every value has a default, so a service only declares what it
changes:

```kotlin
surfMicroservice {
    database {
        enabled = true
        version = "2.+"
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
| `surf.database.version` | `database.version` | `2.+` |
| `surf.microservice.reliability.enabled` | `reliability.enabled` | Enabled when the database is enabled and surf-nats is present |

The boolean properties accept only `true` or `false`. Any other value fails the build.

`database.enabled` adds `surf-microservice-database` and `dev.slne.surf:surf-database-r2dbc` in `database.version` to
`implementation`. Do not call surf-api's `withSurfDatabaseR2dbc(...)` in a microservice: the build fails with a hint,
because surf-api adds the dependency with version `+` and relocates it away from the database integration.

`reliability.enabled` adds `surf-microservice-reliability` to `implementation` and its test fixtures to
`testImplementation`. surf-nats counts as present when the `dev.slne.surf.nats` plugin is applied or
`surf-nats-standalone` is declared in `api`, `implementation` or `runtimeOnly`. Enabling reliability explicitly while
the database is disabled or surf-nats is missing fails the build when the dependencies resolve.

For the `docker` block, see [Generate the Dockerfile](#generate-the-dockerfile).

## Main class and migration discovery

You do not register the main class. The KSP processor scans the module at compile time and writes:

- `META-INF/surf-microservice/main`: the one concrete `Microservice` subclass. The jar's `Main-Class` is a fixed
  launcher that reads this resource.
- `META-INF/surf-microservice/migrations`: every concrete `SurfMigration` class or object of the module. The
  database integration runs them at start.

Compilation fails when the module has no `Microservice` subclass or more than one, or when a discovered class is
neither an `object` nor instantiable through a public no-argument constructor. The class must live in the
microservice module itself, not in a dependency.

## Name and data directory

`Microservice.name` identifies the service in logs, as the database pool name and in the migration history table.
It defaults to the environment variable `SURF_MICROSERVICE_NAME`, otherwise to the class name without the
`Microservice` suffix in kebab case: `PunishMicroservice` becomes `punish`. The start fails unless the name consists
of lowercase letters and digits separated by single hyphens, with at most 40 characters.

`Microservice.dataPath` is the directory for configuration files. It defaults to `SURF_MICROSERVICE_CONFIG_DIR`,
otherwise to `config` in the working directory. Override either property in your class to change the default.

| File | Content |
|---|---|
| `<dataPath>/database.yml` | Database connection and pool, overridable with the `SURF_DATABASE_*` variables of surf-database |
| `<dataPath>/nats/config.yml` | NATS connection and handler limits, overridable with the `SURF_NATS_*` variables of surf-nats |
| `<dataPath>/spark/` | spark profiler data |

## Use the database

The database integration opens the database before `onBootstrap`, migrates it and closes it after `onDisable`.
Access it from `onBootstrap` until the end of `onDisable`:

- `microservice.database` returns the Exposed `R2dbcDatabase`.
- `microservice.databaseApi` returns the owning `DatabaseApi`, for example to run additional migrations. Do not call
  `shutdown()` on it.

Both throw `IllegalStateException` outside that window, for example in a constructor or property initializer.

### Migrations

Migrations run before `onBootstrap`. Their history lives in `flyway_schema_history_<name>`, with every `-` in the
service name replaced by `_`. The integration finds them by convention:

- SQL files in `src/main/resources/db/migration`, such as `V1__create_bans.sql`.
- Every `SurfMigration` class or object of the module, listed by the processor.

If a service needs different SQL per database vendor, put the files in `db/migration/mariadb` and
`db/migration/postgresql`. Once one of these folders exists, only the folder of the configured vendor runs and SQL
files directly in `db/migration` are ignored. The start fails if vendor folders exist but none for the configured
vendor. Both folders share one history table, so keep their versions aligned.

Without migrations found by convention, nothing runs.

### Customize the database

Implement `DatabaseCustomizer` on the microservice class to adjust the defaults. Each hook runs at most once, while
the database integration starts:

```kotlin
class PunishMicroservice : Microservice(), DatabaseCustomizer {
    override fun R2dbcDatabaseConfig.Builder.configureDatabase() {
        // Adjust the Exposed configuration.
    }

    override fun MigrationSpec.configureMigrations() {
        sql("db/extra/${MigrationSpec.VENDOR_PLACEHOLDER}")
    }
}
```

- `configureDatabase` adjusts the Exposed configuration after the defaults have been applied.
- `configureMigrations` adds to or overrides the migrations found by convention. The class loader is already set to
  the one of the microservice class.
- `runMigrations` defaults to `true`. With `true`, migrations run even if the conventions found none, so
  `configureMigrations` must then declare at least one. Set it to `false` for a service that still creates its schema
  with `SchemaUtils`, or that implements `DatabaseCustomizer` only for `configureDatabase` and has no migrations.

### NATS handler limits

surf-nats limits how many handlers of one service registration run at the same time. When surf-nats is on the class
path, the database integration sets these defaults from the maximum pool size:

- Concurrent handlers: twice the pool size.
- Concurrent long-running handlers: half the pool size, at least one.

With the default pool of 10 connections, a registration runs at most 20 handlers, 5 of them long-running. The limits
only change surf-nats defaults: `max-concurrent-handlers`, `max-concurrent-long-handlers` and their `-by-service`
entries in `nats/config.yml`, and the variables `SURF_NATS_MAX_CONCURRENT_HANDLERS` and
`SURF_NATS_MAX_CONCURRENT_LONG_HANDLERS`, take precedence.

## Lifecycle

The launcher owns the only JVM shutdown hook of the process. It starts the service in this order:

1. Register the built-in commands.
2. Create the microservice instance and validate its name.
3. Start surf-api.
4. Start spark.
5. Start the integrations in ascending `order`: database (100), NATS (200), reliability (300). Integrations with the
   same order start by name.
6. Call `onBootstrap`.
7. Open the command socket and the console.

On shutdown, after the `exit` command or `SIGTERM`, it stops in this order:

1. Close the command socket and the console.
2. Call `stopAccepting` on the integrations in descending order. NATS stops accepting incoming events and requests
   and waits for running handlers up to the NATS drain timeout.
3. Call `onDisable`. Publishing, service calls and database access still work here.
4. Call `stop` on the integrations in descending order: reliability, NATS, database.
5. Stop spark, then surf-api, then log4j.

If the start fails, the runtime stops only what already started, in the same order, and calls `onDisable` only if
`onBootstrap` returned. A failing stop step is logged and the shutdown continues. The process exits with `1` after a
failed start and with `0` after a shutdown through `exit`.

### Add an integration

An integration is a resource that starts before and stops after the microservice, such as the database or NATS.
Implement `MicroserviceIntegration` and register it for `java.util.ServiceLoader`, usually with `@AutoService`:

```kotlin
@AutoService(MicroserviceIntegration::class)
class CacheIntegration : MicroserviceIntegration {
    override val name = "cache"
    override val order = MicroserviceIntegration.MESSAGING_ORDER + 10

    override suspend fun start(context: MicroserviceIntegrationContext) {
        // Open the resource. Configuration lives below context.dataPath.
    }

    override suspend fun stop(context: MicroserviceIntegrationContext) {
        // Release the resource.
    }
}
```

`stopAccepting` is optional and runs before `onDisable`. `MicroserviceIntegration.DATABASE_ORDER`,
`MESSAGING_ORDER` and `RELIABILITY_ORDER` hold the orders of the built-in integrations. An exception from `start`
aborts the start of the microservice. Each lifecycle call runs at most once per integration and never concurrently
with another.

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

Register commands through `commands` on the microservice, through `MicroserviceCommands` or with the top-level
function `microserviceCommand`. All three use one registry per JVM and work at any time, including in property
initializers:

```kotlin
class PunishMicroservice : Microservice() {
    override suspend fun onBootstrap(args: List<String>) {
        commands.register("bans", "b", description = "Counts the active bans.") { args ->
            val player = args.firstOrNull() ?: fail("Usage: bans <player>")
            sendLine("Active bans of $player: ${countBans(player)}")
        }
    }
}
```

A command with more logic extends `MicroserviceCommand` and is registered with `commands.register(BansCommand())`.
Registering a name or alias that is already taken throws `IllegalArgumentException`.

Inside a command, `sendLine` and `sendError` write to the caller, `fail(message)` ends the command as failed without
a stack trace, and `requestShutdown()` shuts the service down after the response. Any other exception is logged
with its stack trace and reported as a failure.

### Contribute a health check

Implement `MicroserviceHealthContributor` and register it for `ServiceLoader`, usually with `@AutoService`. `check`
returns one `MicroserviceHealthCheckResult` per active resource, or an empty list when the contributor manages none
right now. An unhealthy result needs a message. The runtime runs all contributors concurrently and reports a
contributor that throws or takes longer than 3 seconds as unhealthy. The database integration contributes `database`
(`SELECT 1` within 2.5 seconds), surf-nats contributes `nats`.

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
- `STOPSIGNAL SIGTERM`. The entrypoint `exec`s java, so the signal reaches the launcher's shutdown hook.
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
| `SURF_MICROSERVICE_NAME` | Class name in kebab case | Service name, see [Name and data directory](#name-and-data-directory) |
| `SURF_MICROSERVICE_CONFIG_DIR` | `config` | Data path for configuration files |
| `SURF_MICROSERVICE_COMMAND_SOCKET` | `/tmp/surf-microservice.sock`, `/app/run/microservice.sock` in the image | Command socket of the runtime and `microservicectl` |
| `SURF_MICROSERVICE_SPARK_PROFILER` | `false` | Starts the spark profiler with the service when `true` |
| `SURF_MICROSERVICE_MEMORY_BUDGET` | `docker.memoryBudget` | `-XX:MaxRAM` for containers without a memory limit, image only |
| `JAVA_OPTS` | Empty | JVM options appended last, image only |
| `MALLOC_ARENA_MAX` | `2` in the image | glibc malloc arena limit |

surf-database reads `SURF_DATABASE_*` and surf-nats reads `SURF_NATS_*` in addition.

## Logging

The runtime ships a `log4j2.xml` that logs to the console and to `log/latest.log`, with daily and size-based
rollover. It sets `shutdownHook="disable"`, because the launcher shuts log4j down as its last step. If a service ships
its own `log4j2.xml`, set the attribute there too:

```xml
<Configuration status="WARN" shutdownHook="disable">
```

Without it, log4j registers its own shutdown hook and can stop logging while the service is still shutting down.

## Idempotent handlers and events after the commit

The module `surf-microservice-reliability` makes persistent NATS handlers run at most once per message and publishes
their events after the commit. The plugin adds it by default, see [Configure the build](#configure-the-build).

The module brings its own integration. It starts after the database and NATS and before `onBootstrap`, and it stops
after `onDisable` and before NATS and the database close. On start it migrates its own table
(`surf_atomic_operations`, history table `flyway_schema_history_surf_reliability`), also when the service sets
`runMigrations = false`. On MariaDB the start fails unless the server is 10.6 or newer and the tables use InnoDB.

Register services with `registerTransactionalService` instead of `registerService`, and publish events from their
handlers with the `TransactionalEvents` of `microservice.events`:

```kotlin
override suspend fun onBootstrap(args: List<String>) {
    client = NatsClient.create("punish")
    client.registerTransactionalService<PunishmentService>(PunishmentServiceImpl(database, events))
}

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

Events published through `microservice.events` are held in memory. Once the handler's transaction has committed, they
are published in publish order with `NatsClient.publish` on the client the service is registered on, so they carry its
`Surf-Origin`: listeners of that client see them as their own (`isFromThisClient`), and
`@NatsListener(ignoreFromThisClient = true)` skips them. A handler that throws or rolls back publishes nothing, and
neither does a redelivery answered with the stored reply. Outside a transactional handler `publish` throws. Use
`NatsClient.publish` there.

Delivery is best effort, as for every core NATS event. If the instance stops between the commit and the publish, or a
publish fails because NATS is unavailable, the event is lost and the failure is logged. There is no retry and nothing
is stored in the database. Listeners that are offline miss events. The event is not validated before the commit, so
an event that is no generated event message or exceeds the payload limit is logged as lost.

Settings come from the `ReliabilityCustomizer` interface on the microservice class:

```kotlin
class PunishMicroservice : Microservice(), ReliabilityCustomizer {
    override val reliabilityConfig = ReliabilityConfig(
        atomicOperations = AtomicOperationsConfig(retention = 7.days),
    )
}
```

Stored replies are kept for `NatsPersistentLimits.REDELIVERY_HORIZON` (14 days) by default, the longest time after
which a message with the same id can reach a handler. A shorter retention lets a late redelivery or dead-letter
replay run the handler again. Expired replies are deleted every hour (`cleanupInterval`).

### Test transactional services

`TransactionalHandlerFixture` from the test fixtures runs code as a transactional handler against a test database, so
a service receives the same `TransactionalEvents` as in production and needs no wrapper around it. NATS need not run,
and nothing is published.

```kotlin
val fixture = TransactionalHandlerFixture.create(databaseApi)
val service = PunishmentServiceImpl(databaseApi.database, fixture.events)

fixture.runInHandler { service.createBan(request) }

assertEquals(1, fixture.publishedOf<BanCreated>().size)
assertEquals(listOf(banId), fixture.published.map { it.aggregateId })
```

`create` migrates the reliability schema. Each `runInHandler` call runs its block in one READ COMMITTED transaction,
which `suspendTransaction` on that database joins. `published` lists the events the committed blocks would publish,
in publish order. A block that throws rolls back and adds nothing. `clear()` forgets the recorded events. Calling
`events.publish` outside `runInHandler` throws, as it does outside a handler in production.

## Upgrade from v2

v3 has no compatibility layer. To move a v2 microservice:

1. In `build.gradle.kts`, apply `dev.slne.surf.api.gradle.standalone` and `dev.slne.surf.nats`. Remove
   `dev.slne.surf.microservice` and, if the service used it, `dev.slne.surf.api.gradle.core`. Remove the microservice
   plugin from the root `buildscript` classpath.
2. Remove `withMicroserviceApi()`, `withRabbitModule(...)` and the other `with*Api()` calls from `surfMicroservice`,
   and remove `withSurfDatabaseR2dbc(...)`. Configure the database in `surfMicroservice { database { } }` instead.
3. Remove `@AutoService(Microservice::class)` from the main class. Make sure the module has exactly one concrete
   `Microservice` subclass.
4. Change the imports from `dev.slne.surf.microservice.api.microservice.*` to `dev.slne.surf.microservice.api.*`,
   `dev.slne.surf.microservice.api.command.*` and `dev.slne.surf.microservice.api.health.*`.
   Replace `command.register()` and `MicroserviceCommandManager.registerCommand(command)` with
   `commands.register(command)` or `MicroserviceCommands.register(command)`. `microserviceCommand(...)` keeps working
   and also takes a `description`.
5. Delete the `dataPath` override if it pointed to `config`, which is the default now. Check that the default
   `name` fits, or override it.
6. Move resource setup from constructors and property initializers into `onBootstrap`.
7. Replace your own `DatabaseApi.create(...)` and `shutdown()` calls with `microservice.database`. Move schema setup
   into migrations, or implement `DatabaseCustomizer` with `runMigrations = false` if the service still uses
   `SchemaUtils`.
8. Remove your own `SurfNatsStandalone` start and shutdown calls. The NATS integration connects before `onBootstrap`
   and disconnects after `onDisable`.
9. Remove your own `help` command. Replace RabbitMQ packets and Redis events with NATS services and events, as
   described in surf-nats.
10. Regenerate the Dockerfile with `generateMicroserviceDockerfile` and set `overwrite = true` once if the old file
    lacks the generated marker. Remove RabbitMQ variables from the deployment.

To verify the upgrade, run `./gradlew :MODULE:runMicroservice` and type `health`. Once the database and NATS are
reachable, the command prints a line that starts with `healthy`.
