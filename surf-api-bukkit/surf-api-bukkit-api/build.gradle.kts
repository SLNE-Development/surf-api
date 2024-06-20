plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi(libs.paper.api)
    compileOnlyApi(libs.paper.mojangapi)
    compileOnlyApi(libs.packetevents.spigot)
    compileOnlyApi(libs.scoreboard.library)
    compileOnlyApi(libs.commandapi.bukkit)
    compileOnlyApi(libs.reflection.remapper)
    compileOnlyApi(libs.more.persistent.data.types)
    api(libs.com.github.stefvanschie.inventoryframework.`if`)
    api(libs.commandapi.bukkit.kotlin)
}

description = "surf-api-bukkit-api"
