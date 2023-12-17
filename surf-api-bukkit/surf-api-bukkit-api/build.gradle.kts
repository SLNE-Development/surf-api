plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    compileOnlyApi(libs.paper.api)
    compileOnlyApi(libs.paper.mojangapi)
    api(project(":surf-api-core-api"))
    compileOnlyApi(libs.packetevents)
    compileOnlyApi(libs.entitylib)
    compileOnlyApi(libs.scoreboard.library)
}

description = "surf-api-bukkit-api"
