plugins {
    id("surf.module-api")
    `api-validation`
}

dependencies {
    api(projects.surfApiMessagesApi)
    api(projects.surfApiAnnotations)

    api(libs.inventory.framework.api)
    compileOnlyApi(libs.adventure.api)
    api(libs.glm)
}

description = "surf-api-inventory-api"
