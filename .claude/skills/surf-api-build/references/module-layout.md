# Module layout and settings.gradle.kts

Source: `settings.gradle.kts`, `tooling/bom/build.gradle.kts`, the six `aggregate/*/build.gradle.kts`.

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

Two guards: a directory without a `build.gradle.kts` and a name already taken both fail the build at
settings time. `include(name)` with a flat name plus an explicit `projectDir` is what keeps the
Gradle path flat.

`includePair(group, domain)` calls `includeModule("$group/$domain/api")` and
`includeModule("$group/$domain/impl")`.

## Every included module

| Directory | Gradle path / published name |
| --- | --- |
| `foundation/annotations` | `surf-api-annotations` |
| `foundation/internal` | `surf-api-internal` |
| `foundation/registry-keys` | `surf-api-registry-keys` |
| `foundation/di/api` | `surf-api-di-api` |
| `foundation/di/impl` | `surf-api-di-impl` |
| `common/core/api` | `surf-api-core-api` |
| `common/core/impl` | `surf-api-core-impl` |
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
| `aggregate/core` | `surf-api-core` |
| `aggregate/paper` | `surf-api-paper` |
| `aggregate/minestom` | `surf-api-minestom` |
| `aggregate/minestom-server` | `surf-api-minestom-server` |
| `aggregate/velocity` | `surf-api-velocity` |
| `aggregate/standalone` | `surf-api-standalone` |
| `tooling/bom` | `surf-api-bom` |
| `tooling/gradle-plugin` | `surf-api-gradle-plugin` |
| `tooling/gradle-plugin/processor` | `surf-api-processor` (explicit `name` override) |
| `local/paper-plugin-test` | `surf-api-paper-plugin-test` (non-CI only) |
| `local/minestom-example` | `surf-api-minestom-example` (non-CI only) |
| `local/minestom-example-plugin` | `surf-api-minestom-example-plugin` (non-CI only) |
| `surf-api-generator` | `surf-api-generator` (non-CI only) |

`tooling/gradle-plugin/processor` is the only place the `name` parameter is used; the derived name
would be `surf-api-gradle-plugin-processor`, and `surf-api-processor` is the coordinate that is
actually published and the one `core-convention` wires in as the KSP processor.

The NMS versions are listed literally in `settings.gradle.kts` (`"v1-21-11", "v26-1", "v26-2",
"v26-3"`) because `NmsVersion` lives in the included build `surf-api-generator/nms-module-generator`
and is not on the settings classpath.

## Conditional and excluded modules

- `val ci = System.getenv("CI")?.toBoolean() ?: false`. When `ci` is false, the three `local/`
  modules and `surf-api-generator` are included; in CI they are not.
- `platform/minestom/luckperms-impl` is included only when `vendor/LuckPerms/settings.gradle` exists.
  That directory is generated from the `LuckPerms-Upstream` submodule plus the patches next to it:
  `git submodule update --init --recursive`, then `./gradlew applyPatches` from `vendor/`. Outside CI
  the build logs a warning and continues; in CI the missing directory is a hard error.

## Included builds

- `pluginManagement { includeBuild("surf-api-generator/nms-module-generator") }` - supplies the
  `dev.slne.surf.api.generator.nms-module-generator` plugin applied in the root `build.gradle.kts`.
- `includeBuild("vendor/spark-minestom")` with `substitute(module("me.lucko:spark-minestom"))`.
- `includeBuild(vendor/LuckPerms)` with `substitute(module("me.lucko.luckperms:minestom"))`, when it
  exists.

The vendor artifacts are built from source and published nowhere, which is why
`vendor-bundle-convention` exists - see `conventions.md`.

## Aggregates

All six are three-line build files. The full set:

| Aggregate | Exports |
| --- | --- |
| `surf-api-core` | `projects.surfApiCoreApi` |
| `surf-api-paper` | `projects.surfApiPaperApi` |
| `surf-api-minestom` | `projects.surfApiMinestomApi` |
| `surf-api-minestom-server` | `projects.surfApiMinestomImpl` - this coordinate builds a runnable server |
| `surf-api-velocity` | `projects.surfApiVelocityApi` |
| `surf-api-standalone` | `projects.surfApiStandaloneImpl` - still re-exports the whole standalone surface; splitting it is a later step |

Each sets `description` to its own published name. `surf.aggregate` registers the `surfRuntime`
configuration for embedding implementations, but no aggregate uses it yet - today they all reach
their dependency through plain `api(...)`.

## The BOM

`tooling/bom/build.gradle.kts` applies `java-platform` and `maven-publish` only - not
`core-convention` - and reads `group`, `version` and `snapshot` from `gradle.properties` itself.

```kotlin
val notPublished = setOf(
    "surf-api-bom",
    "surf-api-gradle-plugin",
    "surf-api-processor",
    "surf-api-generator",
    "surf-api-paper-plugin-test",
    "surf-api-minestom-example",
    "surf-api-minestom-example-plugin",
)
```

Constraints are `api("$group:$it:$version")` for every subproject name not in that set, sorted. The
reasons, from the file's own comment: modules `settings.gradle.kts` only includes outside CI, plus
the Gradle plugin and its KSP processor, which carry versions of their own. `surf-api-bom` excludes
itself.

Because the set is matched against subproject *names*, a module added outside CI that is not in the
set would appear as a BOM constraint only on a local build and never in a CI-built BOM - add it.
