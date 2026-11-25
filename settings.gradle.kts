pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
        // The surf-nats part of the Gradle plugin depends on the weaver plugin published there.
        maven("https://reposilite.slne.dev/public/") { name = "slne-repository-public" }
    }
    includeBuild("tooling/nms-module-generator")
    // An included build rather than a module, so the infra modules of this build can apply the surf
    // plugins the way a downstream project does.
    includeBuild("tooling/gradle-plugin")
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

/**
 * Includes the `api` and `impl` modules of one domain.
 */
fun includePair(group: String, domain: String) {
    includeModule("$group/$domain/api")
    includeModule("$group/$domain/impl")
}

includeModule("foundation/annotations")
includeModule("foundation/internal")
includeModule("foundation/registry-keys")
includePair("foundation", "spring")

includeModule("common/common/api")
includeModule("common/messages/api")
includeModule("common/event/api")
includeModule("common/nbt/api")
includeModule("common/reflection/api")
includeModule("common/command/api")
includeModule("common/luckperms/api")
includePair("common", "core")
includeModule("common/inventory/api")
includeModule("common/serialization/api")
includePair("common", "config")

includePair("platform", "paper")
includeModule("platform/paper/nms/api")
// NmsVersion is compiled by surf-api-internal and the generator build, neither of which is on the
// settings classpath. platform/paper/nms/shared-26 holds shared sources and is no module; v26-3
// compiles them in place, the older 26.x modules compile copies.
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
includeModule("platform/standalone/testing")

listOf("core", "paper", "minestom", "minestom-server", "velocity", "standalone")
    .forEach { includeModule("aggregate/$it") }

includeModule("tooling/bom")

/**
 * Includes an imported repository under `infra/[repository]` as a project tree.
 *
 * Each repository keeps its directory layout and its root build script, whose `subprojects { }`
 * configuration applies to the [modules] below it. [modules] are the repository's former Gradle
 * paths; every segment is renamed from `surf-` to `surf-api-`, so
 * `surf-nats-types:surf-nats-types-common` becomes
 * `:surf-api-nats:surf-api-nats-types:surf-api-nats-types-common`.
 */
fun includeInfra(repository: String, vararg modules: String) {
    val root = "infra/$repository"
    val rootPath = ":surf-api-$repository"
    include(rootPath)
    project(rootPath).projectDir = file(root)

    for (module in modules) {
        val segments = module.split(':')
        val name = infraName(segments.last())
        require(file("$root/${segments.joinToString("/")}/build.gradle.kts").isFile) {
            "No build.gradle.kts in $root/$module"
        }
        require(declared.add(name)) { "Duplicate module name '$name' (module $module)" }

        for (depth in segments.indices) {
            val path = segments.take(depth + 1).joinToString(
                ":",
                prefix = "$rootPath:",
            ) { infraName(it) }
            include(path)
            project(path).projectDir = file("$root/${segments.take(depth + 1).joinToString("/")}")
        }
    }
}

fun infraName(name: String): String = name.replaceFirst("surf-", "surf-api-")

includeInfra(
    "nats",
    "surf-nats-api",
    "surf-nats-core",
    "surf-nats-types:surf-nats-types-common",
    "surf-nats-types:surf-nats-types-paper",
    "surf-nats-spring",
    "surf-nats-test:surf-nats-test-common",
    "surf-nats-test:surf-nats-test-fixtures",
    "surf-nats-test:surf-nats-test-fixtures-internal",
    "surf-nats-test:surf-nats-test-paper",
    "surf-nats-test:surf-nats-test-microservice",
)
includeInfra(
    "redis",
    "surf-redis-api",
    "surf-redis-core",
    "surf-redis-spring",
)
includeInfra(
    "microservice",
    "surf-microservice-api",
    "surf-microservice-runtime",
    "surf-microservice-database",
    "surf-microservice-reliability",
)
includeModule("infra/database-r2dbc", name = "surf-api-database-r2dbc")

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

    if (ci) {
        error(message)
    }

    logger.warn(message)
}

if (!ci) {
    includeModule("local/paper-plugin-test")
    includeModule("local/paper-shared-bean-provider")
    includeModule("local/paper-shared-bean-consumer")
    includeModule("local/paper-shared-bean-test")
    includeModule("local/velocity-shared-bean-provider")
    includeModule("local/velocity-shared-bean-consumer")
    includeModule("local/velocity-shared-bean-test")
    includeModule("local/minestom-example")
    includeModule("local/minestom-example-plugin")
    includeModule("tooling/registry-key-generator")
}
