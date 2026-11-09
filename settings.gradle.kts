pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    includeBuild("surf-api-generator/nms-module-generator")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "surf-api"

val ci = System.getenv("CI")?.toBoolean() ?: false

/**
 * Derives a module's published name from its directory.
 *
 * The first path segment only groups modules and is dropped; the rest is joined with `-` and
 * prefixed with `surf-api-`:
 *
 * ```
 * common/core/api         ->  surf-api-core-api
 * platform/paper/nms/v26-3 -> surf-api-paper-nms-v26-3
 * aggregate/paper         ->  surf-api-paper
 * ```
 */
fun artifactName(path: String): String {
    val segments = path.split('/')

    // A module kept at the repository root already carries its published name as its directory.
    if (segments.size == 1) return segments.single()

    return segments.drop(1).joinToString("-", prefix = "surf-api-")
}

val declared = mutableSetOf<String>()

/**
 * Includes the module at [path] under its published [name].
 *
 * The Gradle path stays flat and the directory is attached as `projectDir`, so the typesafe
 * accessor reads `projects.surfApiCoreApi` rather than `projects.common.core.api` — an `api`
 * accessor sitting next to the `api()` configuration in the same dependencies block would be a
 * trap. It also means the published name needs no `archivesName` override.
 */
fun includeModule(path: String, name: String = artifactName(path)) {
    val dir = file(path)
    require(dir.resolve("build.gradle.kts").isFile) { "No build.gradle.kts in $path" }
    require(declared.add(name)) { "Duplicate module name '$name' (path $path)" }

    include(name)
    project(":$name").projectDir = dir
}

/** Includes the `api` and `impl` modules of one domain. */
fun includePair(group: String, domain: String) {
    includeModule("$group/$domain/api")
    includeModule("$group/$domain/impl")
}

// foundation
includeModule("foundation/annotations")
includeModule("foundation/internal")
includeModule("foundation/registry-keys")
includePair("foundation", "di")

// common
// Only the api half exists: the shared base -- `logger` and the rest of `util`, the transforming
// set views, the voxel line tracer, the weighted random selector and the small caps table -- is
// declarations with no runtime wiring. It is declared first because it is the one module every
// other domain of the repository compiles against.
includeModule("common/common/api")
// Only the api half exists: the palette, the builder DSL, the Adventure extensions, the pagination
// contracts, the configured MiniMessage instance and the `ActionBarService` contract are
// declarations. Their implementations stay in `surf-api-core-impl`, because `ActionBarServiceImpl`
// resolves players through `SurfApiCore`.
includeModule("common/messages/api")
// Only the api half exists: the `SurfEvent` hierarchy, the `@SurfEventHandler` annotation, the
// priorities and the `SurfEventBus` contract are declarations. `SurfEventBusImpl` and its
// hidden-class invoker templates stay in `surf-api-core-impl`; nothing compiles against them, so a
// module of their own would buy no boundary.
includeModule("common/event/api")
// Only the api half exists: the binary-tag views, the `FastCompoundBinaryTag` contract, the
// `InternalNbtBridge` and `NbtOps` are declarations. `InternalNbtBridgeImpl` stays in
// `surf-api-core-impl`.
includeModule("common/nbt/api")
// Only the api half exists: the `SurfReflection` contract and the `@SurfProxy` annotation family
// are declarations, and `SurfReflectionImpl` stays in `surf-api-core-impl`. It is its own module
// rather than part of the nbt one it was planned with, because the two domains share nothing: tags
// on one side, dynamic proxies for the NMS bridges on the other.
includeModule("common/reflection/api")
includePair("common", "core")
// Only the api half exists: the shared inventory framework is contracts, settings and components
// that both platforms compile against, with no runtime wiring of its own.
includeModule("common/inventory/api")
// Only the api half exists: the serializers, the serializer modules and the codec extensions are
// declarations with no runtime wiring, so there is nothing for an impl module to hold.
includeModule("common/serialization/api")
// Both halves exist: the contracts, the constraint annotations and the helper types are compiled
// against, and `SurfConfigApi` is a bean whose implementation has to ship somewhere.
includePair("common", "config")

// platform
includePair("platform", "paper")
includeModule("platform/paper/nms/api")
// NmsVersion lives in the included build and is not on the settings classpath, so the versions
// stay literal here, as they did before.
listOf("v1-21-11", "v26-1", "v26-2", "v26-3").forEach { includeModule("platform/paper/nms/$it") }

includePair("platform", "minestom")
listOf(
    "bootstrap-impl",
    "console-impl",
    "npc-impl",
    "player-visibility-impl",
    "plugins-impl",
    "signed-chat-impl",
    "spark-impl",
).forEach { includeModule("platform/minestom/$it") }

includePair("platform", "velocity")
includeModule("platform/standalone/impl")

// aggregates — the coordinates downstream projects know
listOf("core", "paper", "minestom", "minestom-server", "velocity", "standalone")
    .forEach { includeModule("aggregate/$it") }

// tooling — surf-api-generator stays at the repository root: it is local-only, never published, and
// carries 3,532 registry data files, so moving it would dominate every structural diff.
// tooling
includeModule("tooling/bom")
includeModule("tooling/gradle-plugin")
includeModule("tooling/gradle-plugin/processor", name = "surf-api-processor")

includeBuild("vendor/spark-minestom") {
    dependencySubstitution {
        substitute(module("me.lucko:spark-minestom")).using(project(":"))
    }
}

// vendor/LuckPerms is generated from the LuckPerms-Upstream submodule and the patches next to it,
// so the LuckPerms feature is only built once it has been created; CI builds require it
val luckPermsDir = file("vendor/LuckPerms")
if (luckPermsDir.resolve("settings.gradle").isFile) {
    includeModule("platform/minestom/luckperms-impl")

    includeBuild(luckPermsDir) {
        dependencySubstitution {
            substitute(module("me.lucko.luckperms:minestom")).using(project(":minestom"))
        }
    }
} else {
    val message = """
        vendor/LuckPerms is missing, so surf-api-minestom-luckperms-impl is not built. Create it with:
          git submodule update --init --recursive
          cd vendor && ./gradlew applyPatches
        """.trimIndent()

    if (ci) error(message)
    logger.warn(message)
}

if (!ci) {
    includeModule("local/paper-plugin-test")
    includeModule("local/minestom-example")
    includeModule("local/minestom-example-plugin")
    includeModule("surf-api-generator")
}
