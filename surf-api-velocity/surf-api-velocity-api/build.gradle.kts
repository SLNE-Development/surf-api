plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi(libs.velocity.api)
    compileOnlyApi(libs.packetevents.velocity)
    compileOnlyApi(libs.commandapi.velocity)
}

description = "surf-api-velocity-api"
