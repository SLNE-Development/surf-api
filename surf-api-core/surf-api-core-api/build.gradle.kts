plugins {
    `core-convention`
//    alias(libs.plugins.dokka)
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.adventure.text.minimessage)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.adventure.serializer.legacy)
    compileOnlyApi(libs.adventure.serializer.plain)
    compileOnlyApi(libs.adventure.serializer.ansi)
    api(libs.adventure.nbt)
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
    api(libs.glm)

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

    api(libs.bundles.ktor.client)

    api(libs.datafixerupper) { isTransitive = false }
}

kotlin {
    compilerOptions {
        optIn.add("dev.slne.surf.surfapi.core.api.util.InternalSurfApi")
    }
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-core-api"
