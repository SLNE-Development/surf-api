pluginManagement {
    repositories {
        mavenLocal()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

val ci = System.getenv("CI")?.toBoolean() == true

rootProject.name = "surf-api"
include(":surf-api-core:surf-api-core-api")
include(":surf-api-core:surf-api-core-server")

include(":surf-api-bukkit:surf-api-bukkit-api")
include(":surf-api-bukkit:surf-api-bukkit-server")

//include(":surf-api-hytale:surf-api-hytale-api")
//include(":surf-api-hytale:surf-api-hytale-server")

include(":surf-api-velocity:surf-api-velocity-api")
include(":surf-api-velocity:surf-api-velocity-server")

include("surf-api-standalone")
include("surf-api-gradle-plugin")
include("surf-api-gradle-plugin:surf-api-processor")

if (!ci) {
    include(":surf-api-bukkit:surf-api-bukkit-plugin-test")
//    include("surf-api-generator")
    include("surf-api-modern-generator")
}