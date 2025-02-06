plugins {
    `core-convention`
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
    api(libs.okhttp.kotlin)
    compileOnlyApi(libs.fastutil)
    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.configurate.yaml)
    compileOnlyApi(libs.configurate.jackson)
    api(libs.configurate.kotlin)
    compileOnlyApi(libs.flogger)
    compileOnlyApi(libs.commons.math4.core)
    compileOnlyApi(libs.aide.reflection)

    api(libs.caffeine.courotines)
    api(libs.kotlinxCoroutines.core)
    api(libs.kotlinxCoroutines.reactive)
    api(libs.kotlinxCoroutines.reactor)
    api(libs.kotlin.reflect)
    api(libs.kotlin.serialization.json)

    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.gson)
    compileOnlyApi(libs.commons.lang3)
    compileOnlyApi(libs.commons.text)
    compileOnlyApi(libs.fastutil)
}

description = "surf-api-core-api"
