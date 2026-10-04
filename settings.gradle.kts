pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
    includeBuild("surf-api-generator/gradle-nms-module-generator")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "surf-api"

include(":surf-api-core:surf-api-core")
include(":surf-api-core:surf-api-core-server")

include(":surf-api-paper:surf-api-paper")
include(":surf-api-paper:surf-api-paper-nms:surf-api-paper-nms-common")
include(":surf-api-paper:surf-api-paper-nms:surf-api-paper-nms-v1-21-11")
include(":surf-api-paper:surf-api-paper-nms:surf-api-paper-nms-v26-1")
include(":surf-api-paper:surf-api-paper-nms:surf-api-paper-nms-v26-2")
include(":surf-api-paper:surf-api-paper-nms:surf-api-paper-nms-v26-3")
include(":surf-api-paper:surf-api-paper-server")

include(":surf-api-velocity:surf-api-velocity")
include(":surf-api-velocity:surf-api-velocity-server")

include("surf-api-standalone")
include("surf-api-gradle-plugin")
include("surf-api-gradle-plugin:surf-api-processor")

include("surf-api-shared")
include("surf-api-shared:surf-api-shared-public")
include("surf-api-shared:surf-api-shared-internal")

include(":surf-api-minestom:surf-api-minestom")
include(":surf-api-minestom:surf-api-minestom-server")
include(":surf-api-minestom:surf-api-minestom-server-signed-chat")
include(":surf-api-minestom:surf-api-minestom-server-npc")
include(":surf-api-minestom:surf-api-minestom-server-player-visibility")
include(":surf-api-minestom:surf-api-minestom-server-console")
include(":surf-api-minestom:surf-api-minestom-server-spark")
include(":surf-api-minestom:surf-api-minestom-server-plugins")

includeBuild("vendor/spark-minestom") {
    dependencySubstitution {
        substitute(module("me.lucko:spark-minestom")).using(project(":"))
    }
}

// vendor/LuckPerms is generated from the LuckPerms-Upstream submodule and the patches next to it,
// so the LuckPerms feature is only built once it has been created
val luckPermsDir = file("vendor/LuckPerms")
if (luckPermsDir.resolve("settings.gradle").isFile) {
    include(":surf-api-minestom:surf-api-minestom-server-luckperms")

    includeBuild(luckPermsDir) {
        dependencySubstitution {
            substitute(module("club.tesseract:luckperms-minestom")).using(project(":minestom"))
        }
    }
} else {
    logger.warn(
        """
        vendor/LuckPerms is missing, so surf-api-minestom-server-luckperms is not built. Create it with:
          git submodule update --init --recursive
          cd vendor && ./gradlew applyPatches
        """.trimIndent()
    )
}

val ci = System.getenv("CI")?.toBoolean() ?: false

if (!ci) {
    include(":surf-api-paper:surf-api-paper-plugin-test")
    include(":surf-api-minestom:surf-api-minestom-example")
    include(":surf-api-minestom:surf-api-minestom-example-plugin")
    include("surf-api-generator")
}