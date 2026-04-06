plugins {
    `core-convention`
    `api-validation`
}

dependencies {
    api(projects.surfApiShared.surfApiSharedPublic)
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
    compileOnlyApi(libs.commons.math3)
    compileOnlyApi(libs.aide.reflection)
    api(libs.glm)

    api(libs.caffeine.courotines)
    api(libs.bundles.kotlin.coroutines)
    api(libs.bundles.reactor.netty)

    compileOnlyApi(libs.guava)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.gson)
    compileOnlyApi(libs.commons.lang3)
    compileOnlyApi(libs.commons.text)
    compileOnlyApi(libs.fastutil)

    api(libs.bundles.ktor.client)

    api(libs.datafixerupper) { isTransitive = false }
}

tasks {
    shadowJar {
        val relocationPrefix: String by project
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-core"
