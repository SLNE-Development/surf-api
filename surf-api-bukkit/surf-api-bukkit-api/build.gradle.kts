plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    api(libs.scoreboard.library.api)
    compileOnlyApi(libs.commandapi.paper)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)

    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
}

description = "surf-api-bukkit-api"

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
