plugins {
    id("surf.module-api")
}

dependencies {
    compileOnlyApi(libs.kotlin.stdlib)
    compileOnlyApi(libs.adventure.api)
}

description = "surf-api-registry-keys"
