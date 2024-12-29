plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnly(libs.paper.api)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.scoreboard.library.api)
    compileOnlyApi(libs.commandapi.bukkit)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    compileOnlyApi(libs.inventoryframework)

    api(libs.commandapi.bukkit.kotlin)
    compileOnlyApi(libs.mccoroutine.folia.api)
}

description = "surf-api-bukkit-api"
