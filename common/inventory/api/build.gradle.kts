plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiMessagesApi)
    api(projects.surfApiAnnotations)

    compileOnlyApi(libs.inventory.framework.api)
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.glm)
}

description = "surf-api-inventory-api"
