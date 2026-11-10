# surf-microservice

The `dev.slne.surf.microservice` Gradle plugin configures Surf microservice dependencies and can generate a production-oriented, multi-stage Dockerfile for Coolify.

## Generate a Dockerfile

Run:

```shell
./gradlew generateMicroserviceDockerfile
```

The typed task is registered lazily in the `distribution` group. By default it writes `Dockerfile` in the consuming project root, uses the existing `shadowJar` task in the container build stage, derives Eclipse Temurin JDK/JRE images from the configured Java toolchain, and runs the application as a non-root user. The generated artifact path is relative to the root project, which is the Docker build context; for a subproject such as `surf-clan-microservice`, the generated `COPY` source therefore includes that subproject directory. Additional JVM flags can be supplied with `JAVA_OPTS`; see [JVM settings](#jvm-settings).

The output can be configured through the existing extension:

```kotlin
surfMicroservice {
    withMicroserviceApi()

    docker {
        outputFile.set(layout.projectDirectory.file("deploy/Dockerfile"))
        baseImage.set("eclipse-temurin:25-jre")
        builderImage.set("eclipse-temurin:25-jdk")
        overwrite.set(false)
    }
}
```

The default `overwrite = false` protects manually maintained Dockerfiles. A file carrying the generated marker can be regenerated safely; replacing any other existing file requires explicitly setting `overwrite = true`.

The generated Dockerfile only sets the JVM settings below and the command socket path. Service configuration such as RabbitMQ or database settings is neither documented nor generated into it—configure those as Coolify runtime variables/secrets.

### JVM settings

The container starts java with these options, in this order. For options with a value, such as
`-XX:ActiveProcessorCount=4`, the last one wins. Collector flags add up instead: `JAVA_OPTS=-XX:+UseSerialGC` alone
stops the JVM with "multiple garbage collectors selected", so add `-XX:-UseG1GC` or replace `docker.jvmOptions`.

1. `docker.jvmOptions`. The defaults target HotSpot 25: `-XX:+UseG1GC -XX:+UseCompactObjectHeaders
   -XX:MaxRAMPercentage=60.0 -XX:ActiveProcessorCount=2 -XX:G1PeriodicGCInterval=60000
   -XX:TrimNativeHeapInterval=60000 -XX:+ExitOnOutOfMemoryError`. `jvmOptions.add(...)` appends to them; assigning
   a list replaces them, for example for another JVM or an older Java version, which reject unknown options at
   startup.
2. `-XX:MaxRAM=<budget>`, but only when cgroup v2 shows no memory limit for the container or any of its parent
   cgroups. The budget comes from `SURF_MICROSERVICE_MEMORY_BUDGET`, default `docker.memoryBudget` (`1g`); an empty
   value turns it off. On cgroup v1 hosts the budget is not applied, and an unlimited container sizes the JVM from the
   host memory.
3. `JAVA_OPTS`.

The image also sets `MALLOC_ARENA_MAX=2`, which limits the native memory glibc keeps per malloc arena.

Without a memory limit the JVM sizes its heap from the host memory and keeps what it once used. In a benchmark on
2026-11-09 (12 visible cores, no container limit), a service with the previous options (`-XX:MaxRAMPercentage=75.0`)
held 5.9–6.7 GiB after a load peak. With these defaults it peaked at 710–750 MiB and fell back to about 275 MiB when
idle; with the spark profiler running it stayed at about 490 MiB. A service that needs more memory sets
`SURF_MICROSERVICE_MEMORY_BUDGET=2g` or a container memory limit; one that needs more parallelism sets
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
tree on the heap, so it does not start with the service: in the benchmark it raised the idle memory from about
275 MiB to about 500 MiB and the idle CPU from 0.45 % to 1.5 % of a core. Start it on demand with
`microservicectl spark profiler start`, or with the service by setting `SURF_MICROSERVICE_SPARK_PROFILER=true`; any
value other than `true` or `false` stops the service start.

## Idempotent handlers and events after the commit

The module `surf-microservice-reliability` makes persistent NATS handlers run at most once per message and publishes
their events after the commit. The plugin adds it to `implementation`, and its test fixtures to
`testImplementation`, whenever the database integration is enabled and surf-nats is present: the `dev.slne.surf.nats`
plugin is applied or `surf-nats-standalone` is declared. Turn it off, or on explicitly, in the DSL or with the Gradle
property `surf.microservice.reliability.enabled`:

```kotlin
surfMicroservice {
    reliability {
        enabled = false
    }
}
```

Enabling it explicitly while the database is disabled or surf-nats is missing fails the build with the missing
requirement.

The module brings its own integration. It starts after the database and NATS and before `onBootstrap`, and it stops
after `onDisable` and before NATS and the database close. On start it migrates its own table (`surf_atomic_operations`,
history table `flyway_schema_history_surf_reliability`); this also happens when the service sets
`runMigrations = false`. On MariaDB the start fails unless the server is 10.6 or newer and the tables use InnoDB.

Register services with `registerTransactionalService` instead of `registerService`, and publish events from their
handlers with the `TransactionalEvents` of `microservice.events`:

```kotlin
override suspend fun onBootstrap(args: List<String>) {
    client = NatsClient.create("surf-punish")
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
| Handler throws | Rollback, nothing stored; surf-nats answers the RPC with the error or redelivers the fire call. |
| Database unavailable, lock wait over 10 s, commit fails | No reply; the message is redelivered (`NatsRedeliveryException`). |
| Deadlock after the handler ran | Rollback; the message is redelivered instead of retried in process. |
| Same message id, different request | Error reply `AtomicOperationConflictException`. |

Events published through `microservice.events` are held in memory. Once the handler's transaction has committed, they
are published in publish order with `NatsClient.publish` on the client the service is registered on, so they carry its
`Surf-Origin`: listeners of that client see them as their own (`isFromThisClient`), and
`@NatsListener(ignoreFromThisClient = true)` skips them. A handler that throws or rolls back publishes nothing, and
neither does a redelivery answered with the stored reply. Outside a transactional handler `publish` throws; use
`NatsClient.publish` there.

Delivery is best effort, as for every core NATS event: if the instance stops between the commit and the publish, or a
publish fails because NATS is unavailable, the event is lost and the failure is logged. There is no retry and nothing
is stored in the database. Listeners that are offline miss events, as before. The event is not validated before the
commit, so an event that is no generated event message or exceeds the payload limit is logged as lost.

Settings come from the `ReliabilityCustomizer` interface on the microservice class:

```kotlin
class PunishMicroservice : Microservice(), ReliabilityCustomizer {
    override val reliabilityConfig = ReliabilityConfig(
        atomicOperations = AtomicOperationsConfig(retention = 7.days),
    )
}
```

Stored replies are kept for `NatsPersistentLimits.REDELIVERY_HORIZON` (14 days) by default, the longest time after
which a message with the same id can reach a handler. Expired replies are deleted every hour (`cleanupInterval`).

### Testing transactional services

`TransactionalHandlerFixture` from the test fixtures runs code as a transactional handler against a test database, so
a service receives the same `TransactionalEvents` as in production and needs no wrapper around it. NATS need not run;
nothing is published.

```kotlin
val fixture = TransactionalHandlerFixture.create(databaseApi)
val service = PunishmentServiceImpl(databaseApi.database, fixture.events)

fixture.runInHandler { service.createBan(request) }

assertEquals(1, fixture.publishedOf<BanCreated>().size)
assertEquals(listOf(banId), fixture.published.map { it.aggregateId })
```

`create` migrates the reliability schema. Each `runInHandler` call runs its block in one READ COMMITTED transaction, which
`suspendTransaction` on that database joins. `published` lists the events the committed blocks would publish, in
publish order; a block that throws rolls back and adds nothing.
Calling `events.publish` outside `runInHandler` throws, as it does outside a handler in production.
