import dev.slne.surf.surfapi.PluginType

plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
    id("dev.slne.surf.surfapi") version "1.0.1"
}

surfApi {
    pluginType.set(PluginType.CORE_API)
    internal.set(true)
}

dependencies {
    api(project(":surf-api-core:surf-api-core-api-standalone"))
//    compileOnlyApi(libs.packetevents.api)
//    compileOnlyApi(libs.dazzleconf)
//    compileOnlyApi(libs.spongepowered.math)
//    compileOnlyApi(libs.okhttp)
//    compileOnlyApi(libs.fastutil)
//    compileOnlyApi(libs.commandapi.core)
//    compileOnlyApi(libs.brigadier)
//    compileOnlyApi(libs.configurate.yaml)
//    compileOnlyApi(libs.configurate.jackson)
//    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
}

description = "surf-api-core-api"
