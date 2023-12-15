pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}



rootProject.name = "surf-api"
include(":surf-api-core-api")
include(":surf-api-core")
include(":surf-api-bukkit-api")
include(":surf-api-velocity")
include(":surf-api-bukkit")
include(":surf-api-bukkit-plugin-test")
include(":surf-api-velocity-server")
include(":surf-api-core-server")
include(":surf-api-velocity-api")
include(":surf-api-bukkit-server")
project(":surf-api-core-api").projectDir = file("surf-api-core/surf-api-core-api")
project(":surf-api-bukkit-api").projectDir = file("surf-api-bukkit/surf-api-bukkit-api")
project(":surf-api-bukkit-plugin-test").projectDir = file("surf-api-bukkit/surf-api-bukkit-plugin-test")
project(":surf-api-velocity-server").projectDir = file("surf-api-velocity/surf-api-velocity-server")
project(":surf-api-core-server").projectDir = file("surf-api-core/surf-api-core-server")
project(":surf-api-velocity-api").projectDir = file("surf-api-velocity/surf-api-velocity-api")
project(":surf-api-bukkit-server").projectDir = file("surf-api-bukkit/surf-api-bukkit-server")
