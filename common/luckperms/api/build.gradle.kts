plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiMessagesApi)

    compileOnlyApi(libs.luckperms)
    compileOnlyApi(libs.adventure.api)

    compileOnlyApi(libs.bundles.kotlin.coroutines)
}

description = "surf-api-luckperms-api"
