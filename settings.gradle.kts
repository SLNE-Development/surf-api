pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "surf-api"

include(":surf-api-core:surf-api-core")
include(":surf-api-core:surf-api-core-server")

include(":surf-api-paper:surf-api-paper")
include(":surf-api-paper:surf-api-paper-server")

include(":surf-api-velocity:surf-api-velocity")
include(":surf-api-velocity:surf-api-velocity-server")

include("surf-api-standalone")
include("surf-api-gradle-plugin")
include("surf-api-gradle-plugin:surf-api-processor")

include("surf-api-shared")
include("surf-api-shared:surf-api-shared-public")
include("surf-api-shared:surf-api-shared-internal")

val ci = System.getenv("CI")?.toBoolean() ?: false

if (!ci) {
//    include(":surf-api-paper:surf-api-paper-plugin-test")
    include("surf-api-generator")
}