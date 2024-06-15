pluginManagement {
    // Include 'plugins build' to define convention plugins.
    includeBuild("build-logic")
}



rootProject.name = "surf-api"
include(":surf-api-core:surf-api-core-api")
include(":surf-api-core:surf-api-core-server")

include(":surf-api-bukkit:surf-api-bukkit-api")
include(":surf-api-bukkit:surf-api-bukkit-server")
include(":surf-api-bukkit:surf-api-bukkit-plugin-test")

include(":surf-api-velocity:surf-api-velocity-api")
include(":surf-api-velocity:surf-api-velocity-server")
include("surf-api-generator")
