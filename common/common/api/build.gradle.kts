plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiAnnotations)

    api(libs.kotlin.stdlib)
    api(libs.bundles.kotlin.coroutines)
    api(libs.bundles.kotlin.serialization)

    compileOnlyApi(libs.flogger)
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.text.logger.slf4j)
    compileOnlyApi(libs.slf4j)
    compileOnlyApi(libs.caffeine)
    compileOnlyApi(libs.commons.math3)
    compileOnlyApi(libs.packetevents.api)
    compileOnlyApi(libs.spongepowered.math)
}

description = "surf-api-common-api"
