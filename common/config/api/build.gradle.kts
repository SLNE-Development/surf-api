plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)
    api(projects.surfApiMessagesApi)
    api(projects.surfApiSpringApi)

    compileOnlyApi(libs.bundles.configurate)
    compileOnlyApi(libs.commons.lang3)
}

description = "surf-api-config-api"
