plugins {
    id("dev.slne.java-library-conventions")
    id("dev.slne.java-shadow-conventions")
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.adventure.serializer.legacy)
    compileOnlyApi(libs.adventure.serializer.plain)
    compileOnlyApi(libs.adventure.serializer.ansi)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.dazzleconf)
    compileOnlyApi(libs.spongepowered.math)
    compileOnlyApi(libs.okhttp)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.configurate.yaml)
    compileOnlyApi(libs.configurate.jackson)
    compileOnlyApi(libs.flogger)
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC")
}

description = "surf-api-core-api"
