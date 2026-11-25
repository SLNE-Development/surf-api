plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiMessagesApi)

    compileOnlyApi(libs.commandapi.core)
    compileOnlyApi(libs.brigadier)
    compileOnlyApi(libs.adventure.api)

    compileOnlyApi(libs.bundles.kotlin.coroutines)
}

description = "surf-api-command-api"
