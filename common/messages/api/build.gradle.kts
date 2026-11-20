plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)
    api(projects.surfApiAnnotations)
    api(projects.surfApiSpringApi)
    api(libs.bundles.kotlin.coroutines)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.brigadier) { isTransitive = false }
}

description = "surf-api-messages-api"
