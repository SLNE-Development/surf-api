import dev.slne.surf.surfapi.PluginType

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    id("dev.slne.surf.surfapi") version "1.0.1"
}

surfApi {
    pluginType.set(PluginType.VELOCITY_API)
    internal.set(true)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api"))
    compileOnlyApi(libs.velocity.api)
//    compileOnlyApi(libs.packetevents.velocity)
//    compileOnlyApi(libs.commandapi.velocity)
}

description = "surf-api-velocity-api"
