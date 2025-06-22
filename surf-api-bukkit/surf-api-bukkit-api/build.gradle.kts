plugins {
    `core-convention`
//    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    api(libs.scoreboard.library.api)
    compileOnlyApi(libs.commandapi.bukkit)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    compileOnlyApi(libs.inventoryframework)

    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
    api(libs.querz.nbt)
}

description = "surf-api-bukkit-api"

configurations.all {
    exclude(group = "org.spigotmc", module = "spigot-api")
}
