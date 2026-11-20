plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiCommonApi)
    api(projects.surfApiMessagesApi)
    api(projects.surfApiSpringApi)

    api(libs.configurate.yaml)
    api(libs.configurate.jackson)
    api(libs.configurate.kotlin)
    compileOnlyApi(libs.commons.lang3)
}

description = "surf-api-config-api"
