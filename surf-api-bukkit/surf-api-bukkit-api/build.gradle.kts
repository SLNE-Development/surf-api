plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi(libs.paper.api)
    compileOnlyApi(libs.paper.mojangapi)
    compileOnlyApi(libs.packetevents)
    compileOnlyApi(libs.entitylib)
    compileOnlyApi(libs.scoreboard.library)
}

description = "surf-api-bukkit-api"
