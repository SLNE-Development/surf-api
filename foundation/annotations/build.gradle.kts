plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    compileOnlyApi(libs.bundles.adventure)
    compileOnlyApi(libs.slf4j)

    compileOnlyApi(libs.kotlin.reflect)
    compileOnlyApi(libs.bundles.kotlin.serialization)
}