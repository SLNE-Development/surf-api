pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    includeBuild("tooling/generator/nms-module-generator")
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
fun artifactName(path: String) =
    path.split('/').drop(1).joinToString("-", prefix = "surf-api-")

val declared = mutableSetOf<String>()

/**
 * Includes the module at [path] under its published [name].
 *
 * The Gradle path stays flat and the directory is attached as `projectDir`, so the typesafe
 * accessor reads `projects.surfApiCoreApi` rather than `projects.common.core.api` — an `api`
 * accessor sitting next to the `api()` configuration in the same dependencies block would be a
 * trap. It also means the published name needs no `archivesName` override.
 */
fun module(path: String, name: String = artifactName(path)) {
    val dir = file(path)
    require(dir.resolve("build.gradle.kts").isFile) { "No build.gradle.kts in $path" }
    require(declared.add(name)) { "Duplicate module name '$name' (path $path)" }

    include(name)
    project(":$name").projectDir = dir
}

/** Includes the `api` and `impl` modules of one domain. */
fun pair(group: String, domain: String) {
    module("$group/$domain/api")
    module("$group/$domain/impl")
}

// foundation
module("foundation/annotations")
module("foundation/internal")

// common
pair("common", "core")

// platform
pair("platform", "paper")
module("platform/paper/nms/api")
// NmsVersion lives in the included build and is not on the settings classpath, so the versions
// stay literal here, as they did before.
listOf("v1-21-11", "v26-1", "v26-2", "v26-3").forEach { module("platform/paper/nms/$it") }

pair("platform", "minestom")
listOf(
    "bootstrap-impl",
    "console-impl",
    "npc-impl",
    "player-visibility-impl",
    "plugins-impl",
    "signed-chat-impl",
    "spark-impl",
).forEach { module("platform/minestom/$it") }

pair("platform", "velocity")
module("platform/standalone/impl")

// aggregates — the coordinates downstream projects know
listOf("core", "paper", "minestom", "minestom-server", "velocity", "standalone")
    .forEach { module("aggregate/$it") }

// tooling
module("tooling/bom")
module("tooling/gradle-plugin")
module("tooling/gradle-plugin/processor", name = "surf-api-processor")

includeBuild("vendor/spark-minestom") {
    dependencySubstitution {
        substitute(module("me.lucko:spark-minestom")).using(project(":"))
    }
}

// vendor/LuckPerms is generated from the LuckPerms-Upstream submodule and the patches next to it,
// so the LuckPerms feature is only built once it has been created; CI builds require it
val luckPermsDir = file("vendor/LuckPerms")
if (luckPermsDir.resolve("settings.gradle").isFile) {
    module("platform/minestom/luckperms-impl")

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
    module("local/paper-plugin-test")
    module("local/minestom-example")
    module("local/minestom-example-plugin")
    module("tooling/generator")
}
