# Module layout and settings.gradle.kts

Source: `settings.gradle.kts`, the root `build.gradle.kts`, `tooling/bom/build.gradle.kts` and the
six `aggregate/*/build.gradle.kts`.

## Naming

```kotlin
fun artifactName(path: String): String {
    val segments = path.split('/')
    if (segments.size == 1) return segments.single()
    return segments.drop(1).joinToString("-", prefix = "surf-api-")
}
```

The first segment is a grouping directory and is dropped. A one-segment path is a module kept at the
repository root that already carries its published name as its directory.

```kotlin
fun includeModule(path: String, name: String = artifactName(path)) {
    val dir = file(path)
    require(dir.resolve("build.gradle.kts").isFile) { "No build.gradle.kts in $path" }
    require(declared.add(name)) { "Duplicate module name '$name' (path $path)" }

    include(name)
    project(":$name").projectDir = dir
}
```

Two guards: a directory without a `build.gradle.kts` and a name already taken both fail at settings
time. `include(name)` with a flat name plus an explicit `projectDir` keeps the Gradle path flat, so
the typesafe accessor reads `projects.surfApiCoreApi` rather than `projects.common.core.api`.

`includePair(group, domain)` calls `includeModule("$group/$domain/api")` and
`includeModule("$group/$domain/impl")`.

The `name` argument is passed once: `infra/database-r2dbc` becomes `surf-api-database-r2dbc`.

## Every module included with includeModule

| Directory | Gradle path / published name |
| --- | --- |
| `foundation/annotations` | `surf-api-annotations` |
| `foundation/internal` | `surf-api-internal` |
| `foundation/registry-keys` | `surf-api-registry-keys` |
| `foundation/spring/api` | `surf-api-spring-api` |
| `foundation/spring/impl` | `surf-api-spring-impl` |
| `common/common/api` | `surf-api-common-api` |
| `common/messages/api` | `surf-api-messages-api` |
| `common/event/api` | `surf-api-event-api` |
| `common/nbt/api` | `surf-api-nbt-api` |
| `common/reflection/api` | `surf-api-reflection-api` |
| `common/command/api` | `surf-api-command-api` |
| `common/luckperms/api` | `surf-api-luckperms-api` |
| `common/core/api` | `surf-api-core-api` |
| `common/core/impl` | `surf-api-core-impl` |
| `common/inventory/api` | `surf-api-inventory-api` |
| `common/serialization/api` | `surf-api-serialization-api` |
| `common/config/api` | `surf-api-config-api` |
| `common/config/impl` | `surf-api-config-impl` |
| `platform/paper/api` | `surf-api-paper-api` |
| `platform/paper/impl` | `surf-api-paper-impl` |
| `platform/paper/nms/api` | `surf-api-paper-nms-api` |
| `platform/paper/nms/v1-21-11` | `surf-api-paper-nms-v1-21-11` |
| `platform/paper/nms/v26-1` | `surf-api-paper-nms-v26-1` |
| `platform/paper/nms/v26-2` | `surf-api-paper-nms-v26-2` |
| `platform/paper/nms/v26-3` | `surf-api-paper-nms-v26-3` |
| `platform/minestom/api` | `surf-api-minestom-api` |
| `platform/minestom/impl` | `surf-api-minestom-impl` |
| `platform/minestom/bootstrap-impl` | `surf-api-minestom-bootstrap-impl` |
| `platform/minestom/console-impl` | `surf-api-minestom-console-impl` |
| `platform/minestom/npc-impl` | `surf-api-minestom-npc-impl` |
| `platform/minestom/player-visibility-impl` | `surf-api-minestom-player-visibility-impl` |
| `platform/minestom/plugins-impl` | `surf-api-minestom-plugins-impl` |
| `platform/minestom/signed-chat-impl` | `surf-api-minestom-signed-chat-impl` |
| `platform/minestom/spark-impl` | `surf-api-minestom-spark-impl` |
| `platform/minestom/luckperms-impl` | `surf-api-minestom-luckperms-impl` (conditional, see below) |
| `platform/velocity/api` | `surf-api-velocity-api` |
| `platform/velocity/impl` | `surf-api-velocity-impl` |
| `platform/standalone/impl` | `surf-api-standalone-impl` |
| `platform/standalone/testing` | `surf-api-standalone-testing` |
| `aggregate/core` | `surf-api-core` |
| `aggregate/paper` | `surf-api-paper` |
| `aggregate/minestom` | `surf-api-minestom` |
| `aggregate/minestom-server` | `surf-api-minestom-server` |
| `aggregate/velocity` | `surf-api-velocity` |
| `aggregate/standalone` | `surf-api-standalone` |
| `tooling/bom` | `surf-api-bom` |
| `infra/database-r2dbc` | `surf-api-database-r2dbc` (explicit `name`) |
| `local/paper-plugin-test` | `surf-api-paper-plugin-test` (non-CI only) |
| `local/paper-shared-bean-provider` | `surf-api-paper-shared-bean-provider` (non-CI only) |
| `local/paper-shared-bean-consumer` | `surf-api-paper-shared-bean-consumer` (non-CI only) |
| `local/paper-shared-bean-test` | `surf-api-paper-shared-bean-test` (non-CI only) |
| `local/paper-server-harness` | `surf-api-paper-server-harness` (non-CI only) |
| `local/paper-scenario-probe` | `surf-api-paper-scenario-probe` (non-CI only) |
| `local/paper-scenario-owner` | `surf-api-paper-scenario-owner` (non-CI only) |
| `local/paper-scenario-test` | `surf-api-paper-scenario-test` (non-CI only) |
| `local/velocity-shared-bean-provider` | `surf-api-velocity-shared-bean-provider` (non-CI only) |
| `local/velocity-shared-bean-consumer` | `surf-api-velocity-shared-bean-consumer` (non-CI only) |
| `local/velocity-shared-bean-test` | `surf-api-velocity-shared-bean-test` (non-CI only) |
| `local/minestom-example` | `surf-api-minestom-example` (non-CI only) |
| `local/minestom-example-plugin` | `surf-api-minestom-example-plugin` (non-CI only) |
| `tooling/registry-key-generator` | `surf-api-registry-key-generator` (non-CI only) |

The README of each common api-only domain says where its implementation lives. The NMS versions
are listed literally (`"v1-21-11", "v26-1", "v26-2", "v26-3"`), because `NmsVersion` is compiled
by `surf-api-internal` and the included build `tooling/nms-module-generator`, neither of
which is on the settings classpath.

The `local/*-shared-bean-*` modules are two test plugins and a harness per platform that boot a
real Paper server or Velocity proxy; they stay local because the harness downloads the server.
`local/paper-scenario-test` drives bots against a real Paper server with the probe plugin of
`local/paper-scenario-probe` and the session owner plugin of `local/paper-scenario-owner`. Both
Paper tests share the server assembly of `local/paper-server-harness`.

## The infra trees

```kotlin
fun includeInfra(repository: String, vararg modules: String)
fun infraName(name: String) = name.replaceFirst("surf-", "surf-api-")
```

`includeInfra` includes `infra/<repository>` as `:surf-api-<repository>` and every module below it
under its former Gradle path, each segment renamed by `infraName`. Each segment becomes a project,
so the parents only group:

| Repository | Modules (Gradle path below `:surf-api-<repository>`) |
| --- | --- |
| `nats` | `surf-api-nats-api`, `surf-api-nats-core`, `surf-api-nats-types:surf-api-nats-types-common`, `surf-api-nats-types:surf-api-nats-types-paper`, `surf-api-nats-spring`, and five `surf-api-nats-test:*` modules |
| `redis` | `surf-api-redis-api`, `surf-api-redis-core`, `surf-api-redis-spring` |
| `microservice` | `surf-api-microservice-api`, `surf-api-microservice-runtime`, `surf-api-microservice-database`, `surf-api-microservice-reliability` |

The accessor follows the nesting: `projects.surfApiNats.surfApiNatsTypes.surfApiNatsTypesPaper`.
The duplicate-name guard covers the leaf names together with the `includeModule` names.

Each repository keeps its own root `build.gradle.kts`, whose `subprojects { }` block configures its
modules. `infra/redis/build.gradle.kts`, for example, relocates Redisson, Netty and their
dependencies in every shadow jar and excludes Redisson's `META-INF/spring.handlers`,
`spring.schemas` and `spring.tooling`, which name an unrelocated handler class absent from the jar.
Most modules apply the surf Gradle plugin rather than the buildSrc roles; `surf-nats-api` and
`surf-nats-core` are Kotlin Multiplatform modules configured by `infra/nats/build.gradle.kts`. The
root build gives every project under `infra/` the surf-api group and version.

## Conditional and excluded modules

- `val ci = System.getenv("CI")?.toBoolean() ?: false`. When `ci` is false, the `local/` modules
  and `tooling/registry-key-generator` are included; in CI they are not.
- `platform/minestom/luckperms-impl` is included only when `vendor/LuckPerms/settings.gradle`
  exists. That directory is generated from the `LuckPerms-Upstream` submodule plus the patches next
  to it: `git submodule update --init --recursive`, then `./gradlew applyPatches` from `vendor/`.
  Outside CI the build logs a warning and continues; in CI the missing directory is a hard error.

## Included builds

- `pluginManagement { includeBuild("tooling/nms-module-generator") }` supplies the
  `dev.slne.surf.api.generator.nms-module-generator` plugin applied in the root `build.gradle.kts`.
- `pluginManagement { includeBuild("tooling/gradle-plugin") }` supplies the surf Gradle plugins,
  so the infra modules apply them the way a downstream project does. The root build registers
  `publish` and `publishToMavenLocal` tasks that delegate to it, and makes `check` depend on its
  `check`.
- `includeBuild("vendor/spark-minestom")` with `substitute(module("me.lucko:spark-minestom"))`.
- `includeBuild(vendor/LuckPerms)` with `substitute(module("me.lucko.luckperms:minestom"))`, when
  it exists.

The vendor artifacts are built from source and published nowhere, which is why
`vendor-bundle-convention` exists - see `conventions.md`.

## Coordinate substitution

The root `build.gradle.kts` substitutes every request for `dev.slne.surf.api:<name>` in every
configuration with the project of that name, so the surf Gradle plugins and the infra builds can
request modules by coordinate the way a downstream build does. A module that publishes a `shadow`
publication (the shaded infra jars) is substituted with its `SHADOWED` variant, because outside
this build its coordinate resolves to that jar.

## Aggregates

| Aggregate | Exports |
| --- | --- |
| `surf-api-core` | `projects.surfApiCoreApi`, `projects.surfApiConfigApi` |
| `surf-api-paper` | `projects.surfApiPaperApi` |
| `surf-api-minestom` | `projects.surfApiMinestomApi` |
| `surf-api-minestom-server` | `projects.surfApiMinestomImpl` - this coordinate builds a runnable server |
| `surf-api-velocity` | `projects.surfApiVelocityApi` |
| `surf-api-standalone` | `projects.surfApiStandaloneImpl` - the whole standalone surface; `SurfApiStandaloneBootstrap` creates the root context and cannot be split off |

Each sets `description` to its own published name and reaches its dependencies through plain
`api(...)`.

## The BOM

`tooling/bom/build.gradle.kts` applies `java-platform` and `maven-publish` only - not
`core-convention` - and reads `group`, `version` and `snapshot` from `gradle.properties` itself.

```kotlin
val notPublished = setOf(
    "surf-api-bom",
    "surf-api-registry-key-generator",
    "surf-api-paper-plugin-test",
    "surf-api-minestom-example",
    "surf-api-minestom-example-plugin",
    "surf-api-paper-shared-bean-provider",
    "surf-api-paper-shared-bean-consumer",
    "surf-api-paper-shared-bean-test",
    "surf-api-paper-server-harness",
    "surf-api-paper-scenario-probe",
    "surf-api-paper-scenario-owner",
    "surf-api-paper-scenario-test",
    "surf-api-velocity-shared-bean-provider",
    "surf-api-velocity-shared-bean-consumer",
    "surf-api-velocity-shared-bean-test",
)
```

Constraints are `api("$group:$it:$version")` for every leaf subproject name not in that set,
sorted; the infra parents only group modules and are skipped. The set is matched against
subproject *names*, so a module included only outside CI that is missing from it appears as a
constraint in a locally built BOM and never in a CI-built one.
