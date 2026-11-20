plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(libs.kotlin.stdlib)
    api(libs.bundles.kotlin.serialization)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.adventure.serializer.gson)
    compileOnlyApi(libs.spongepowered.math)
}

description = "surf-api-serialization-api"
