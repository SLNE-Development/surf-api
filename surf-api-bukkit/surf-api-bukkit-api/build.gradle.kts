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
}

description = "surf-api-bukkit-api"
