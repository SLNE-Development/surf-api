plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)
    api(projects.surfApiAnnotations)
    api(projects.surfApiSpringApi)
    compileOnlyApi(libs.bundles.kotlin.coroutines)

    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.brigadier) { isTransitive = false }

    testImplementation(libs.adventure.serializer.plain)
}

description = "surf-api-messages-api"
