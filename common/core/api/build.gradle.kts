plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiRegistryKeys)
    api(projects.surfApiAnnotations)
    // The DI contracts travel with the api, not with the container: a downstream plugin writes
    // @Service and takes a BeanContainer at its entry point, and it compiles against surf-api-core
    // like everything else. Which module exports these IS the sharing decision — one exporter, so
    // the interface a SHARED bean crosses the application boundary as is loaded exactly once.
    api(projects.surfApiDiApi)
    api(libs.adventure.nbt)
    // Exposed transitively by surf-api-annotations as well; declared here so the test convention
    // (testImplementation extendsFrom compileOnlyApi) puts Adventure on the test classpath.
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.luckperms)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.dazzleconf)
    compileOnlyApi(libs.spongepowered.math)
    compileOnlyApi(libs.okhttp)
    api(libs.okhttp.kotlin)
    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    api(libs.configurate.yaml)
    api(libs.configurate.jackson)
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

    api(libs.bundles.ktor.client)

    api(libs.datafixerupper) { isTransitive = false }
}

tasks {
    shadowJar {
        val relocationPrefix = project.findProperty("relocationPrefix") as String
        relocate("com.mojang.serialization", "$relocationPrefix.mojang.serialization")
        relocate("com.mojang.datafixers", "$relocationPrefix.mojang.datafixers")
    }
}

description = "surf-api-core-api"
