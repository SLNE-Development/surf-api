plugins {
    `core-convention`
//    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.scoreboard.library.api) { isTransitive = false }
    compileOnlyApi(libs.commandapi.bukkit)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    compileOnlyApi(libs.inventoryframework)

    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
}

description = "surf-api-bukkit-api"
